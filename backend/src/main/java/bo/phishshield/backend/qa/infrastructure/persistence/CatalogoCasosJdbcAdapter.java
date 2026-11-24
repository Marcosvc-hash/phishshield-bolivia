package bo.phishshield.backend.qa.infrastructure.persistence;

import bo.phishshield.backend.qa.domain.model.CasoPrueba;
import bo.phishshield.backend.qa.domain.model.Herramienta;
import bo.phishshield.backend.qa.domain.model.Suite;
import bo.phishshield.backend.qa.domain.port.out.CatalogoCasosPort;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/** Catalogo de casos (qa_casos) con SQL directo. */
@Component
public class CatalogoCasosJdbcAdapter implements CatalogoCasosPort {

    static final String COLUMNAS = """
            c.id_caso, c.codigo, c.suite, c.herramienta, c.orden, c.nombre, c.objetivo,
            c.precondiciones, c.datos_prueba, c.resultado_esperado, c.activo
            """;

    static final RowMapper<CasoPrueba> A_CASO = (rs, n) -> new CasoPrueba(
            rs.getLong("id_caso"),
            rs.getString("codigo"),
            Suite.desdeValorBd(rs.getString("suite")),
            Herramienta.desdeValorBd(rs.getString("herramienta")),
            rs.getInt("orden"),
            rs.getString("nombre"),
            rs.getString("objetivo"),
            rs.getString("precondiciones"),
            rs.getString("datos_prueba"),
            rs.getString("resultado_esperado"),
            rs.getBoolean("activo"));

    /** Las suites en el orden del enum (smoke, sanity, ...) y dentro de cada una por orden y codigo. */
    private static final Comparator<CasoPrueba> ORDEN_CATALOGO = Comparator
            .comparing((CasoPrueba c) -> c.suite().ordinal())
            .thenComparingInt(CasoPrueba::orden)
            .thenComparing(CasoPrueba::codigo);

    private final JdbcClient jdbc;

    public CatalogoCasosJdbcAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<CasoPrueba> listarActivos() {
        return jdbc.sql("SELECT " + COLUMNAS + " FROM qa_casos c WHERE c.activo")
                .query(A_CASO)
                .list()
                .stream()
                .sorted(ORDEN_CATALOGO)
                .toList();
    }

    @Override
    public List<CasoPrueba> listarPorSuite(Suite suite) {
        return jdbc.sql("SELECT " + COLUMNAS + " FROM qa_casos c WHERE c.activo AND c.suite = :suite"
                        + " ORDER BY c.orden, c.codigo")
                .param("suite", suite.aValorBd())
                .query(A_CASO)
                .list();
    }

    @Override
    public List<CasoPrueba> buscarPorCodigos(Collection<String> codigos) {
        if (codigos == null || codigos.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("SELECT " + COLUMNAS + " FROM qa_casos c WHERE c.codigo IN (:codigos)")
                .param("codigos", List.copyOf(codigos))
                .query(A_CASO)
                .list()
                .stream()
                .sorted(ORDEN_CATALOGO)
                .toList();
    }
}
