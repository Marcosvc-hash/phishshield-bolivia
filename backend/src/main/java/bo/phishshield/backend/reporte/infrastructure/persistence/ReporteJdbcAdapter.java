package bo.phishshield.backend.reporte.infrastructure.persistence;

import bo.phishshield.backend.reporte.domain.model.EmpresaDelReporte;
import bo.phishshield.backend.reporte.domain.model.PeriodoReporte;
import bo.phishshield.backend.reporte.domain.model.ReporteEmpresa;
import bo.phishshield.backend.reporte.domain.model.TotalesReporte;
import bo.phishshield.backend.reporte.domain.port.out.ReporteRepositoryPort;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.Pagina;
import bo.phishshield.backend.shared.domain.model.SolicitudPagina;
import bo.phishshield.backend.shared.infrastructure.persistence.FiltrosSql;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Reportes por empresa con SQL directo (JdbcClient), al estilo de los demas
 * *JdbcAdapter. La tabla reportes ya existia; V14 le agrego uuid_publico.
 *
 * Los totales se cuentan sobre analisis.fecha_analisis dentro del periodo
 * (en hora de Bolivia); las alertas son las de esos mismos analisis.
 */
@Component
public class ReporteJdbcAdapter implements ReporteRepositoryPort {

    private static final String SELECT_REPORTE = """
            SELECT r.id_reporte, r.uuid_publico, r.periodo_inicio, r.periodo_fin, r.total_analisis,
                   r.total_phishing, r.total_alertas, r.fecha_generacion, u.email AS generado_por,
                   e.id_empresa, e.uuid_publico AS uuid_empresa, e.nombre AS empresa, e.activa
            FROM reportes r
            JOIN empresas e      ON e.id_empresa = r.id_empresa
            LEFT JOIN usuarios u ON u.id_usuario = r.generado_por
            """;

    private static final RowMapper<EmpresaDelReporte> A_EMPRESA = (rs, n) -> new EmpresaDelReporte(
            rs.getLong("id_empresa"), rs.getObject("uuid_publico", UUID.class), rs.getString("nombre"),
            rs.getBoolean("activa"));

    private static final RowMapper<ReporteEmpresa> A_REPORTE = (rs, n) -> new ReporteEmpresa(
            rs.getLong("id_reporte"),
            rs.getObject("uuid_publico", UUID.class),
            new EmpresaDelReporte(rs.getLong("id_empresa"), rs.getObject("uuid_empresa", UUID.class),
                    rs.getString("empresa"), rs.getBoolean("activa")),
            new PeriodoReporte(rs.getObject("periodo_inicio", LocalDate.class), rs.getObject("periodo_fin", LocalDate.class)),
            new TotalesReporte(rs.getInt("total_analisis"), rs.getInt("total_phishing"), rs.getInt("total_alertas")),
            rs.getString("generado_por"),
            rs.getObject("fecha_generacion", OffsetDateTime.class));

    private final JdbcClient jdbc;

    public ReporteJdbcAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<EmpresaDelReporte> buscarEmpresaPorUuid(UUID uuidPublico) {
        return jdbc.sql("SELECT id_empresa, uuid_publico, nombre, activa FROM empresas WHERE uuid_publico = :id")
                .param("id", uuidPublico)
                .query(A_EMPRESA)
                .optional();
    }

    @Override
    public Optional<EmpresaDelReporte> buscarEmpresaPorId(long idEmpresa) {
        return jdbc.sql("SELECT id_empresa, uuid_publico, nombre, activa FROM empresas WHERE id_empresa = :id")
                .param("id", idEmpresa)
                .query(A_EMPRESA)
                .optional();
    }

    @Override
    public TotalesReporte calcularTotales(long idEmpresa, PeriodoReporte periodo) {
        return jdbc.sql("""
                        SELECT COUNT(*)                                          AS analisis,
                               COUNT(*) FILTER (WHERE a.resultado = 'phishing')  AS phishing,
                               COUNT(al.id_alerta)                               AS alertas
                        FROM analisis a
                        LEFT JOIN alertas al ON al.id_analisis = a.id_analisis
                        WHERE a.id_empresa = :empresa
                          AND a.fecha_analisis >= :inicio
                          AND a.fecha_analisis <  :fin
                        """)
                .param("empresa", idEmpresa)
                .param("inicio", periodo.inicio())
                .param("fin", periodo.finExclusivo())
                .query((rs, n) -> new TotalesReporte(rs.getInt("analisis"), rs.getInt("phishing"), rs.getInt("alertas")))
                .single();
    }

    /**
     * Un solo reporte por empresa y periodo (uq_reportes_periodo): si ya existe,
     * se actualizan sus totales, quien lo genero y la fecha. "xmax = 0" es la
     * forma de PostgreSQL de saber si la fila se inserto (y no se actualizo).
     */
    @Override
    public Guardado guardar(long idEmpresa, PeriodoReporte periodo, TotalesReporte totales, long idUsuario) {
        record Fila(UUID uuid, boolean nuevo) {
        }
        Fila fila = jdbc.sql("""
                        INSERT INTO reportes (id_empresa, periodo_inicio, periodo_fin, total_analisis, total_phishing,
                                              total_alertas, generado_por, fecha_generacion)
                        VALUES (:empresa, :desde, :hasta, :analisis, :phishing, :alertas, :usuario, NOW())
                        ON CONFLICT (id_empresa, periodo_inicio, periodo_fin) DO UPDATE
                        SET total_analisis = EXCLUDED.total_analisis,
                            total_phishing = EXCLUDED.total_phishing,
                            total_alertas = EXCLUDED.total_alertas,
                            generado_por = EXCLUDED.generado_por,
                            fecha_generacion = EXCLUDED.fecha_generacion
                        RETURNING uuid_publico, (xmax = 0) AS nuevo
                        """)
                .param("empresa", idEmpresa)
                .param("desde", periodo.desde())
                .param("hasta", periodo.hasta())
                .param("analisis", totales.analisis())
                .param("phishing", totales.phishing())
                .param("alertas", totales.alertas())
                .param("usuario", idUsuario)
                .query((rs, n) -> new Fila(rs.getObject("uuid_publico", UUID.class), rs.getBoolean("nuevo")))
                .single();
        return new Guardado(buscarPorUuid(fila.uuid()).orElseThrow(), fila.nuevo());
    }

    @Override
    public Optional<ReporteEmpresa> buscarPorUuid(UUID uuidPublico) {
        return jdbc.sql(SELECT_REPORTE + "WHERE r.uuid_publico = :id")
                .param("id", uuidPublico)
                .query(A_REPORTE)
                .optional();
    }

    @Override
    public Pagina<ReporteEmpresa> listar(AlcanceDatos alcance, Long idEmpresa, SolicitudPagina pagina) {
        String filtros = """
                WHERE %s
                  AND (CAST(:filtro AS BIGINT) IS NULL OR r.id_empresa = :filtro)
                """.formatted(FiltrosSql.DE_LA_EMPRESA.formatted("r.id_empresa"));

        long total = jdbc.sql("SELECT COUNT(*) FROM reportes r " + filtros)
                .param("empresa", alcance.idEmpresa())
                .param("filtro", idEmpresa)
                .query(Long.class)
                .single();

        List<ReporteEmpresa> filas = jdbc.sql(SELECT_REPORTE + filtros + """
                        ORDER BY r.fecha_generacion DESC, r.id_reporte DESC
                        LIMIT :limite OFFSET :salto
                        """)
                .param("empresa", alcance.idEmpresa())
                .param("filtro", idEmpresa)
                .param("limite", pagina.tamano())
                .param("salto", pagina.desplazamiento())
                .query(A_REPORTE)
                .list();

        return Pagina.de(filas, pagina, total);
    }
}
