package bo.phishshield.backend.panel.infrastructure.persistence;

import bo.phishshield.backend.panel.domain.model.AnalisisDia;
import bo.phishshield.backend.panel.domain.model.AnalisisReciente;
import bo.phishshield.backend.panel.domain.model.EntidadTop;
import bo.phishshield.backend.panel.domain.model.ResumenEmpresa;
import bo.phishshield.backend.panel.domain.model.ResumenPanel;
import bo.phishshield.backend.panel.domain.port.out.PanelQueryPort;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Consultas del panel con SQL directo (JdbcClient) sobre las vistas:
 *   v_analisis_por_dia y v_top_entidades_suplantadas (migracion V8),
 *   v_resumen_empresa (ya existia en 01_schema.sql).
 *
 * Filtro por empresa: "CAST(:empresa AS BIGINT) IS NULL OR ... = :empresa".
 * Con alcance global el parametro es null y la condicion deja pasar todo.
 * El CAST es necesario porque PostgreSQL no adivina el tipo de un null.
 */
@Component
public class PanelJdbcAdapter implements PanelQueryPort {

    private static final String DE_LA_EMPRESA = "(CAST(:empresa AS BIGINT) IS NULL OR %s = :empresa)";

    private final JdbcClient jdbc;

    public PanelJdbcAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Las alertas se cuentan con las tablas y no con v_alertas_abiertas
     * porque esa vista no trae id_empresa (solo el nombre, que no es unico).
     */
    @Override
    public ResumenPanel resumen(AlcanceDatos alcance) {
        String sql = """
                SELECT
                  (SELECT COALESCE(SUM(total), 0)    FROM v_analisis_por_dia WHERE %1$s) AS total,
                  (SELECT COALESCE(SUM(phishing), 0) FROM v_analisis_por_dia WHERE %1$s) AS phishing,
                  (SELECT COUNT(*) FROM alertas al
                     JOIN analisis an ON an.id_analisis = al.id_analisis
                    WHERE al.estado IN ('pendiente', 'en_revision') AND %2$s)        AS alertas,
                  (SELECT COUNT(*) FROM usuarios u
                    WHERE u.estado = 'activo' AND %3$s)                               AS usuarios
                """.formatted(
                DE_LA_EMPRESA.formatted("id_empresa"),
                DE_LA_EMPRESA.formatted("an.id_empresa"),
                DE_LA_EMPRESA.formatted("u.id_empresa"));

        return jdbc.sql(sql)
                .param("empresa", alcance.idEmpresa())
                .query((rs, n) -> new ResumenPanel(
                        rs.getLong("total"),
                        rs.getLong("phishing"),
                        rs.getLong("alertas"),
                        rs.getLong("usuarios")))
                .single();
    }

    @Override
    public List<AnalisisDia> analisisPorDia(AlcanceDatos alcance, LocalDate desde) {
        String sql = """
                SELECT dia, SUM(total) AS total, SUM(phishing) AS phishing,
                       SUM(sospechosos) AS sospechosos, SUM(seguros) AS seguros
                FROM v_analisis_por_dia
                WHERE dia >= :desde AND %s
                GROUP BY dia
                ORDER BY dia
                """.formatted(DE_LA_EMPRESA.formatted("id_empresa"));

        return jdbc.sql(sql)
                .param("desde", desde)
                .param("empresa", alcance.idEmpresa())
                .query((rs, n) -> new AnalisisDia(
                        rs.getObject("dia", LocalDate.class),
                        rs.getLong("total"),
                        rs.getLong("phishing"),
                        rs.getLong("sospechosos"),
                        rs.getLong("seguros")))
                .list();
    }

    @Override
    public List<EntidadTop> topEntidades(AlcanceDatos alcance, int limite) {
        String sql = """
                SELECT entidad, tipo, SUM(veces_suplantada) AS veces
                FROM v_top_entidades_suplantadas
                WHERE %s
                GROUP BY id_entidad, entidad, tipo
                ORDER BY veces DESC, entidad
                LIMIT :limite
                """.formatted(DE_LA_EMPRESA.formatted("id_empresa"));

        return jdbc.sql(sql)
                .param("empresa", alcance.idEmpresa())
                .param("limite", limite)
                .query((rs, n) -> new EntidadTop(
                        rs.getString("entidad"),
                        rs.getString("tipo"),
                        rs.getLong("veces")))
                .list();
    }

    @Override
    public List<AnalisisReciente> ultimosAnalisis(AlcanceDatos alcance, int limite) {
        String sql = """
                SELECT a.uuid_publico, a.contenido, a.dominio, a.resultado, a.nivel_riesgo,
                       a.fecha_analisis, u.email, e.nombre AS empresa
                FROM analisis a
                LEFT JOIN usuarios u ON u.id_usuario = a.id_usuario
                LEFT JOIN empresas e ON e.id_empresa = a.id_empresa
                WHERE %s
                ORDER BY a.fecha_analisis DESC
                LIMIT :limite
                """.formatted(DE_LA_EMPRESA.formatted("a.id_empresa"));

        return jdbc.sql(sql)
                .param("empresa", alcance.idEmpresa())
                .param("limite", limite)
                .query((rs, n) -> new AnalisisReciente(
                        rs.getObject("uuid_publico", UUID.class),
                        rs.getString("contenido"),
                        rs.getString("dominio"),
                        rs.getString("resultado"),
                        rs.getBigDecimal("nivel_riesgo"),
                        rs.getObject("fecha_analisis", OffsetDateTime.class),
                        rs.getString("email"),
                        rs.getString("empresa")))
                .list();
    }

    @Override
    public List<ResumenEmpresa> resumenPorEmpresa() {
        return jdbc.sql("""
                        SELECT empresa, total_analisis, total_phishing, riesgo_promedio, ultimo_analisis
                        FROM v_resumen_empresa
                        ORDER BY total_analisis DESC, empresa
                        """)
                .query((rs, n) -> new ResumenEmpresa(
                        rs.getString("empresa"),
                        rs.getLong("total_analisis"),
                        rs.getLong("total_phishing"),
                        rs.getBigDecimal("riesgo_promedio"),
                        rs.getObject("ultimo_analisis", OffsetDateTime.class)))
                .list();
    }
}
