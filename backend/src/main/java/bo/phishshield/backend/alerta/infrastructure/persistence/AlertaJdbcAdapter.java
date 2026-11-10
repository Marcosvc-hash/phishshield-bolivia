package bo.phishshield.backend.alerta.infrastructure.persistence;

import bo.phishshield.backend.alerta.domain.model.AlertaFila;
import bo.phishshield.backend.alerta.domain.model.AlertaGestionada;
import bo.phishshield.backend.alerta.domain.model.EstadoAlerta;
import bo.phishshield.backend.alerta.domain.model.FiltroAlertas;
import bo.phishshield.backend.alerta.domain.model.SeveridadAlerta;
import bo.phishshield.backend.alerta.domain.port.out.AlertaRepositoryPort;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.Pagina;
import bo.phishshield.backend.shared.domain.model.SolicitudPagina;
import bo.phishshield.backend.shared.infrastructure.persistence.FiltrosSql;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Bandeja de alertas con SQL directo. Se usa JdbcClient y no la
 * AlertaEntity de la vertical analisis: aquella solo mapea lo que escribe
 * el motor, y asi Hibernate (ddl-auto: validate) no depende de la columna
 * uuid_publico de la migracion V9.
 *
 * La empresa de una alerta es la de su analisis (alertas no tiene id_empresa).
 */
@Component
public class AlertaJdbcAdapter implements AlertaRepositoryPort {

    private static final String SELECT_FILA = """
            SELECT al.uuid_publico, an.uuid_publico AS uuid_analisis, al.severidad, al.estado,
                   an.contenido, an.dominio, an.nivel_riesgo, e.nombre AS empresa,
                   u.email AS usuario, asig.email AS asignado, al.notas,
                   al.fecha_notificacion, al.fecha_cierre
            """;

    private static final String DESDE = """
            FROM alertas al
            JOIN analisis an      ON an.id_analisis = al.id_analisis
            LEFT JOIN empresas e  ON e.id_empresa   = an.id_empresa
            LEFT JOIN usuarios u  ON u.id_usuario   = an.id_usuario
            LEFT JOIN usuarios asig ON asig.id_usuario = al.id_asignado_a
            """;

    private static final String FILTROS = """
            WHERE %s
              AND (CAST(:estado AS VARCHAR) IS NULL OR al.estado = :estado)
              AND (CAST(:severidad AS VARCHAR) IS NULL OR al.severidad = :severidad)
            """.formatted(FiltrosSql.DE_LA_EMPRESA.formatted("an.id_empresa"));

    private static final RowMapper<AlertaFila> A_FILA = (rs, n) -> new AlertaFila(
            rs.getObject("uuid_publico", UUID.class),
            rs.getObject("uuid_analisis", UUID.class),
            rs.getString("severidad"),
            rs.getString("estado"),
            rs.getString("contenido"),
            rs.getString("dominio"),
            rs.getBigDecimal("nivel_riesgo"),
            rs.getString("empresa"),
            rs.getString("usuario"),
            rs.getString("asignado"),
            rs.getString("notas"),
            rs.getObject("fecha_notificacion", OffsetDateTime.class),
            rs.getObject("fecha_cierre", OffsetDateTime.class));

    private final JdbcClient jdbc;

    public AlertaJdbcAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Pagina<AlertaFila> buscar(AlcanceDatos alcance, FiltroAlertas filtro, SolicitudPagina pagina) {
        long total = conFiltros(jdbc.sql("SELECT COUNT(*) " + DESDE + FILTROS), alcance, filtro)
                .query(Long.class)
                .single();

        List<AlertaFila> filas = conFiltros(jdbc.sql(SELECT_FILA + DESDE + FILTROS + """
                        ORDER BY CASE al.estado WHEN 'pendiente' THEN 0 WHEN 'en_revision' THEN 1 ELSE 2 END,
                                 al.fecha_notificacion DESC, al.id_alerta DESC
                        LIMIT :limite OFFSET :salto
                        """), alcance, filtro)
                .param("limite", pagina.tamano())
                .param("salto", pagina.desplazamiento())
                .query(A_FILA)
                .list();

        return Pagina.de(filas, pagina, total);
    }

    @Override
    public long contarPendientes(AlcanceDatos alcance) {
        return jdbc.sql("""
                        SELECT COUNT(*)
                        FROM alertas al
                        JOIN analisis an ON an.id_analisis = al.id_analisis
                        WHERE al.estado = 'pendiente' AND %s
                        """.formatted(FiltrosSql.DE_LA_EMPRESA.formatted("an.id_empresa")))
                .param("empresa", alcance.idEmpresa())
                .query(Long.class)
                .single();
    }

    @Override
    public Optional<AlertaGestionada> buscarPorUuid(UUID id) {
        return jdbc.sql("""
                        SELECT al.id_alerta, al.uuid_publico, an.id_empresa, al.severidad, al.estado,
                               al.id_asignado_a, al.notas, al.fecha_notificacion, al.fecha_cierre
                        FROM alertas al
                        JOIN analisis an ON an.id_analisis = al.id_analisis
                        WHERE al.uuid_publico = :id
                        """)
                .param("id", id)
                .query((rs, n) -> new AlertaGestionada(
                        rs.getLong("id_alerta"),
                        rs.getObject("uuid_publico", UUID.class),
                        rs.getObject("id_empresa", Long.class),
                        SeveridadAlerta.desdeValorBd(rs.getString("severidad")),
                        EstadoAlerta.desdeValorBd(rs.getString("estado")),
                        rs.getObject("id_asignado_a", Long.class),
                        rs.getString("notas"),
                        rs.getObject("fecha_notificacion", OffsetDateTime.class),
                        rs.getObject("fecha_cierre", OffsetDateTime.class)))
                .optional();
    }

    @Override
    public Optional<AlertaFila> buscarFila(UUID id) {
        return jdbc.sql(SELECT_FILA + DESDE + "WHERE al.uuid_publico = :id")
                .param("id", id)
                .query(A_FILA)
                .optional();
    }

    /** "AND estado = :leido" es el control optimista: si otro la cambio, no se actualiza ninguna fila. */
    @Override
    public boolean actualizar(AlertaGestionada alerta, EstadoAlerta estadoLeido) {
        int filas = jdbc.sql("""
                        UPDATE alertas
                        SET estado = :estado, id_asignado_a = :asignado, notas = :notas, fecha_cierre = :cierre
                        WHERE id_alerta = :id AND estado = :leido
                        """)
                .param("estado", alerta.getEstado().aValorBd())
                .param("asignado", alerta.getIdAsignado())
                .param("notas", alerta.getNotas())
                .param("cierre", alerta.getFechaCierre())
                .param("id", alerta.getId())
                .param("leido", estadoLeido.aValorBd())
                .update();
        return filas == 1;
    }

    private static JdbcClient.StatementSpec conFiltros(JdbcClient.StatementSpec sql,
                                                      AlcanceDatos alcance, FiltroAlertas filtro) {
        return sql
                .param("empresa", alcance.idEmpresa())
                .param("estado", filtro.estado() == null ? null : filtro.estado().aValorBd())
                .param("severidad", filtro.severidad() == null ? null : filtro.severidad().aValorBd());
    }
}
