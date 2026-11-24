package bo.phishshield.backend.qa.domain.model;

import java.util.Arrays;
import java.util.List;

/**
 * Suites del Centro de Pruebas QA. Cada suite pertenece a un solo tipo de
 * prueba (igual que chk_qa_casos_suite en la base) y tiene el prefijo que
 * llevan los codigos de sus casos: TC-SMK-001 es un caso de SMOKE.
 */
public enum Suite {

    SMOKE(TipoPrueba.FUNCIONAL, "Smoke", "SMK"),
    SANITY(TipoPrueba.FUNCIONAL, "Sanity", "SAN"),
    REGRESSION(TipoPrueba.FUNCIONAL, "Regression", "REG"),
    INTEGRATION(TipoPrueba.FUNCIONAL, "Integration", "INT"),
    E2E(TipoPrueba.FUNCIONAL, "E2E", "E2E"),
    PERFORMANCE(TipoPrueba.NO_FUNCIONAL, "Performance / carga", "PERF"),
    ESTRES(TipoPrueba.NO_FUNCIONAL, "Estrés", "STR"),
    SOAK(TipoPrueba.NO_FUNCIONAL, "Soak (resistencia)", "SOAK"),
    ROBUSTEZ_IA(TipoPrueba.NO_FUNCIONAL, "Robustez del modelo IA", "ROB");

    private final TipoPrueba tipo;
    private final String etiqueta;
    private final String prefijo;

    Suite(TipoPrueba tipo, String etiqueta, String prefijo) {
        this.tipo = tipo;
        this.etiqueta = etiqueta;
        this.prefijo = prefijo;
    }

    public TipoPrueba tipo() {
        return tipo;
    }

    public String etiqueta() {
        return etiqueta;
    }

    /** Lo que va entre "TC-" y el numero en el codigo de cada caso. */
    public String prefijo() {
        return prefijo;
    }

    /** Las suites de un tipo, en el orden en que se muestran. */
    public static List<Suite> delTipo(TipoPrueba tipo) {
        return Arrays.stream(values()).filter(s -> s.tipo == tipo).toList();
    }

    public String aValorBd() {
        return ValorBd.de(this);
    }

    public static Suite desdeValorBd(String valor) {
        return ValorBd.convertir(Suite.class, valor, "la suite");
    }
}
