package bo.phishshield.backend.auditoria.infrastructure.persistence;

import bo.phishshield.backend.auditoria.domain.model.FiltroAuditoria;
import bo.phishshield.backend.auditoria.domain.model.RegistroAuditoria;
import bo.phishshield.backend.auditoria.domain.port.out.AuditoriaQueryPort;
import bo.phishshield.backend.shared.domain.model.Pagina;
import bo.phishshield.backend.shared.domain.model.SolicitudPagina;
import bo.phishshield.backend.shared.infrastructure.persistence.FiltrosSql;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Consultas del registro de accesos con SQL directo.
 *
 * El email se toma de la cuenta (usuarios.email) y, si la fila no tiene
 * usuario, del detalle JSONB: asi aparecen tambien los intentos de login
 * con emails que no existen. "->>" saca un campo del JSON como texto.
 */
@Component
public class AuditoriaJdbcQueryAdapter implements AuditoriaQueryPort {

    private static final TypeReference<Map<String, Object>> MAPA = new TypeReference<>() { };

    private static final String DESDE_Y_FILTROS = """
            FROM auditoria au
            LEFT JOIN usuarios u ON u.id_usuario = au.id_usuario
            WHERE (CAST(:accion AS VARCHAR) IS NULL OR au.accion = :accion)
              AND (CAST(:usuario AS VARCHAR) IS NULL
                   OR u.email ILIKE :usuario ESCAPE '\\'
                   OR au.detalle ->> 'email' ILIKE :usuario ESCAPE '\\')
              AND (CAST(:desde AS TIMESTAMPTZ) IS NULL OR au.creado_en >= :desde)
              AND (CAST(:hasta AS TIMESTAMPTZ) IS NULL OR au.creado_en < :hasta)
            """;

    private final JdbcClient jdbc;
    private final JsonMapper json;

    public AuditoriaJdbcQueryAdapter(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public Pagina<RegistroAuditoria> buscar(FiltroAuditoria filtro, SolicitudPagina pagina) {
        long total = conFiltros(jdbc.sql("SELECT COUNT(*) " + DESDE_Y_FILTROS), filtro)
                .query(Long.class)
                .single();

        List<RegistroAuditoria> filas = conFiltros(jdbc.sql("""
                        SELECT au.creado_en, au.accion, au.entidad, au.detalle::text AS detalle,
                               host(au.ip_origen) AS ip,
                               COALESCE(u.email, au.detalle ->> 'email') AS email
                        """ + DESDE_Y_FILTROS + """
                        ORDER BY au.creado_en DESC, au.id_evento DESC
                        LIMIT :limite OFFSET :salto
                        """), filtro)
                .param("limite", pagina.tamano())
                .param("salto", pagina.desplazamiento())
                .query((rs, n) -> new RegistroAuditoria(
                        rs.getObject("creado_en", OffsetDateTime.class),
                        rs.getString("accion"),
                        rs.getString("email"),
                        rs.getString("entidad"),
                        aMapa(rs.getString("detalle")),
                        rs.getString("ip")))
                .list();

        return Pagina.de(filas, pagina, total);
    }

    @Override
    public List<String> acciones() {
        return jdbc.sql("SELECT DISTINCT accion FROM auditoria ORDER BY accion")
                .query(String.class)
                .list();
    }

    private Map<String, Object> aMapa(String detalle) {
        return detalle == null ? Map.of() : json.readValue(detalle, MAPA);
    }

    private static JdbcClient.StatementSpec conFiltros(JdbcClient.StatementSpec sql, FiltroAuditoria filtro) {
        return sql
                .param("accion", filtro.accion())
                .param("usuario", FiltrosSql.contiene(filtro.usuario()))
                .param("desde", filtro.fechas().inicio())
                .param("hasta", filtro.fechas().finExclusivo());
    }
}
