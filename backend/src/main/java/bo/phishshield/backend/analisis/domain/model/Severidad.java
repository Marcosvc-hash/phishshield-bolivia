package bo.phishshield.backend.analisis.domain.model;

import java.math.BigDecimal;

/**
 * Severidad de una alerta. Coincide con chk_alertas_severidad;
 * 'baja' existe en la base pero el motor no la genera.
 */
public enum Severidad {

    MEDIA,
    ALTA,
    CRITICA;

    private static final BigDecimal UMBRAL_CRITICA = new BigDecimal("90");
    private static final BigDecimal UMBRAL_ALTA = new BigDecimal("70");

    /** Critica si el riesgo es >= 90, alta si es >= 70, media en otro caso. */
    public static Severidad desdePuntaje(BigDecimal nivelRiesgo) {
        if (nivelRiesgo == null) {
            throw new IllegalArgumentException("El nivel de riesgo no puede ser nulo");
        }
        if (nivelRiesgo.compareTo(UMBRAL_CRITICA) >= 0) {
            return CRITICA;
        }
        if (nivelRiesgo.compareTo(UMBRAL_ALTA) >= 0) {
            return ALTA;
        }
        return MEDIA;
    }

    public String aValorBd() {
        return name().toLowerCase();
    }
}
