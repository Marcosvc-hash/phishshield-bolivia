package bo.phishshield.backend.reporte.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Totales del periodo, calculados desde la base: los analisis de la empresa,
 * cuantos fueron phishing y cuantas alertas abrieron esos analisis.
 * Mismas reglas que chk_reportes_totales.
 */
public record TotalesReporte(int analisis, int phishing, int alertas) {

    public TotalesReporte {
        if (analisis < 0 || phishing < 0 || alertas < 0) {
            throw new IllegalArgumentException("Los totales del reporte no pueden ser negativos");
        }
        if (phishing > analisis) {
            throw new IllegalArgumentException("El reporte no puede tener más phishing (" + phishing
                    + ") que análisis (" + analisis + ")");
        }
    }

    /** Porcentaje de phishing sobre los analisis, con un decimal; 0 si no hubo analisis. */
    public BigDecimal porcentajePhishing() {
        if (analisis == 0) {
            return BigDecimal.ZERO.setScale(1);
        }
        return BigDecimal.valueOf(phishing * 100L).divide(BigDecimal.valueOf(analisis), 1, RoundingMode.HALF_UP);
    }
}
