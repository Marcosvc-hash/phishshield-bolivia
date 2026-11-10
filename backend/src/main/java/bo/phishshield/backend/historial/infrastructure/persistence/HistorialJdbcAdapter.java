package bo.phishshield.backend.historial.infrastructure.persistence;

import bo.phishshield.backend.historial.domain.model.AnalisisFila;
import bo.phishshield.backend.historial.domain.model.DetalleAnalisis;
import bo.phishshield.backend.historial.domain.model.DetalleAnalisis.IndicadorDetalle;
import bo.phishshield.backend.historial.domain.model.FiltroHistorial;
import bo.phishshield.backend.historial.domain.port.out.HistorialQueryPort;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.Pagina;
import bo.phishshield.backend.shared.domain.model.SolicitudPagina;
import bo.phishshield.backend.shared.infrastructure.persistence.FiltrosSql;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Historial con SQL directo (JdbcClient), igual que el panel.
 *
 * Cada filtro opcional sigue el patron "(parametro IS NULL OR condicion)":
 * si el parametro llega null, la condicion deja pasar todo. Asi hay un solo
 * SQL fijo y nunca se arma texto con lo que escribio el usuario (los valores
 * van siempre como parametros, lo que evita la inyeccion SQL).
 */
@Component
public class HistorialJdbcAdapter implements HistorialQueryPort {

    private static final String DESDE_Y_FILTROS = """
            FROM analisis a
            LEFT JOIN usuarios u ON u.id_usuario = a.id_usuario
            LEFT JOIN empresas e ON e.id_empresa = a.id_empresa
            LEFT JOIN alertas al ON al.id_analisis = a.id_analisis
            WHERE %s
              AND (CAST(:resultado AS VARCHAR) IS NULL OR a.resultado = :resultado)
              AND (CAST(:desde AS TIMESTAMPTZ) IS NULL OR a.fecha_analisis >= :desde)
              AND (CAST(:hasta AS TIMESTAMPTZ) IS NULL OR a.fecha_analisis < :hasta)
              AND (CAST(:texto AS VARCHAR) IS NULL
                   OR a.dominio ILIKE :texto ESCAPE '\\'
                   OR a.contenido ILIKE :texto ESCAPE '\\')
            """.formatted(FiltrosSql.DE_LA_EMPRESA.formatted("a.id_empresa"));

    private final JdbcClient jdbc;

    public HistorialJdbcAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Dos consultas: la pagina pedida y el total, con el mismo WHERE. */
    @Override
    public Pagina<AnalisisFila> buscar(AlcanceDatos alcance, FiltroHistorial filtro, SolicitudPagina pagina) {
        long total = conFiltros(jdbc.sql("SELECT COUNT(*) " + DESDE_Y_FILTROS), alcance, filtro)
                .query(Long.class)
                .single();

        List<AnalisisFila> filas = conFiltros(jdbc.sql("""
                        SELECT a.uuid_publico, a.contenido, a.dominio, a.resultado, a.nivel_riesgo,
                               a.fecha_analisis, u.email, e.nombre AS empresa, al.estado AS estado_alerta
                        """ + DESDE_Y_FILTROS + """
                        ORDER BY a.fecha_analisis DESC, a.id_analisis DESC
                        LIMIT :limite OFFSET :salto
                        """), alcance, filtro)
                .param("limite", pagina.tamano())
                .param("salto", pagina.desplazamiento())
                .query((rs, n) -> new AnalisisFila(
                        rs.getObject("uuid_publico", UUID.class),
                        rs.getString("contenido"),
                        rs.getString("dominio"),
                        rs.getString("resultado"),
                        rs.getBigDecimal("nivel_riesgo"),
                        rs.getObject("fecha_analisis", OffsetDateTime.class),
                        rs.getString("email"),
                        rs.getString("empresa"),
                        rs.getString("estado_alerta")))
                .list();

        return Pagina.de(filas, pagina, total);
    }

    @Override
    public Optional<DetalleAnalisis> detalle(UUID id) {
        List<IndicadorDetalle> indicadores = jdbc.sql("""
                        SELECT r.codigo, r.descripcion, i.valor, i.aporte
                        FROM analisis_indicadores i
                        JOIN analisis a         ON a.id_analisis = i.id_analisis
                        JOIN reglas_deteccion r ON r.id_regla    = i.id_regla
                        WHERE a.uuid_publico = :id
                        ORDER BY i.aporte DESC, r.codigo
                        """)
                .param("id", id)
                .query((rs, n) -> new IndicadorDetalle(
                        rs.getString("codigo"),
                        rs.getString("descripcion"),
                        rs.getString("valor"),
                        rs.getBigDecimal("aporte")))
                .list();

        return jdbc.sql("""
                        SELECT a.uuid_publico, a.id_empresa, a.contenido, a.dominio, a.resultado,
                               a.nivel_riesgo, a.origen, a.tiempo_ms, a.fecha_analisis,
                               u.email, e.nombre AS empresa,
                               m.nombre || ' ' || m.version AS modelo,
                               al.estado AS estado_alerta, al.severidad
                        FROM analisis a
                        LEFT JOIN usuarios u   ON u.id_usuario = a.id_usuario
                        LEFT JOIN empresas e   ON e.id_empresa = a.id_empresa
                        LEFT JOIN modelos_ia m ON m.id_modelo  = a.id_modelo
                        LEFT JOIN alertas al   ON al.id_analisis = a.id_analisis
                        WHERE a.uuid_publico = :id
                        """)
                .param("id", id)
                .query((rs, n) -> new DetalleAnalisis(
                        rs.getObject("uuid_publico", UUID.class),
                        rs.getObject("id_empresa", Long.class),
                        rs.getString("contenido"),
                        rs.getString("dominio"),
                        rs.getString("resultado"),
                        rs.getBigDecimal("nivel_riesgo"),
                        rs.getString("origen"),
                        rs.getObject("tiempo_ms", Integer.class),
                        rs.getObject("fecha_analisis", OffsetDateTime.class),
                        rs.getString("email"),
                        rs.getString("empresa"),
                        rs.getString("modelo"),
                        rs.getString("estado_alerta"),
                        rs.getString("severidad"),
                        indicadores))
                .optional();
    }

    private static JdbcClient.StatementSpec conFiltros(JdbcClient.StatementSpec sql,
                                                      AlcanceDatos alcance, FiltroHistorial filtro) {
        return sql
                .param("empresa", alcance.idEmpresa())
                .param("resultado", filtro.resultado() == null ? null : filtro.resultado().aValorBd())
                .param("desde", filtro.fechas().inicio())
                .param("hasta", filtro.fechas().finExclusivo())
                .param("texto", FiltrosSql.contiene(filtro.texto()));
    }
}
