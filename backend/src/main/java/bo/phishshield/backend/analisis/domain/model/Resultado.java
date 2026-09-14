package bo.phishshield.backend.analisis.domain.model;

import java.math.BigDecimal;

public enum Resultado {

    SEGURO,
    SOSPECHOSO,
    PHISHING;

    private static final BigDecimal UMBRAL_PHISHING = new BigDecimal("70");
    private static final BigDecimal UMBRAL_SOSPECHOSO = new BigDecimal("40");

    /**
     * Unico lugar donde se decide el veredicto.
     * Espeja la restriccion chk_analisis_coherencia de la base de datos.
     */
    public static Resultado desdePuntaje(BigDecimal nivelRiesgo) {
        if (nivelRiesgo == null) {
            throw new IllegalArgumentException("El nivel de riesgo no puede ser nulo");
        }
        if (nivelRiesgo.compareTo(UMBRAL_PHISHING) >= 0) {
            return PHISHING;
        }
        if (nivelRiesgo.compareTo(UMBRAL_SOSPECHOSO) >= 0) {
            return SOSPECHOSO;
        }
        return SEGURO;
    }

    public String aValorBd() {
        return name().toLowerCase();
    }

    public static Resultado desdeValorBd(String valor) {
        if (valor == null) {
            throw new IllegalArgumentException("Resultado nulo desde la base de datos");
        }
        return valueOf(valor.toUpperCase());
    }
}