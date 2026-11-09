package bo.phishshield.backend.panel.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Los cuatro numeros de las tarjetas del panel. */
public record ResumenPanel(long totalAnalisis,
                           long totalPhishing,
                           long alertasAbiertas,
                           long usuariosActivos) {

    public ResumenPanel {
        if (totalAnalisis < 0 || totalPhishing < 0 || alertasAbiertas < 0 || usuariosActivos < 0) {
            throw new IllegalArgumentException("Los totales del panel no pueden ser negativos");
        }
        if (totalPhishing > totalAnalisis) {
            throw new IllegalArgumentException("No puede haber mas phishing que analisis");
        }
    }

    /** Porcentaje con un decimal; 0 si todavia no hay analisis (evita dividir entre cero). */
    public BigDecimal porcentajePhishing() {
        if (totalAnalisis == 0) {
            return BigDecimal.ZERO.setScale(1);
        }
        return BigDecimal.valueOf(totalPhishing * 100L)
                .divide(BigDecimal.valueOf(totalAnalisis), 1, RoundingMode.HALF_UP);
    }
}
