package bo.phishshield.backend.analisis.domain.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lo que respondio el modelo de IA para una URL.
 *
 * @param probabilidad probabilidad de phishing, entre 0 y 1
 * @param umbral       desde que probabilidad el propio modelo la considera phishing
 * @param senales      senales que empujan hacia phishing, de la mas fuerte a la mas debil
 * @param modelo       nombre del modelo (modelos_ia.nombre)
 * @param version      version del modelo (modelos_ia.version)
 */
public record PrediccionIa(BigDecimal probabilidad, BigDecimal umbral, List<String> senales,
                           String modelo, String version) {

    public PrediccionIa {
        if (probabilidad == null || probabilidad.signum() < 0 || probabilidad.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("La probabilidad debe estar entre 0 y 1: " + probabilidad);
        }
        if (umbral == null || umbral.signum() < 0 || umbral.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("El umbral debe estar entre 0 y 1: " + umbral);
        }
        senales = senales == null ? List.of() : List.copyOf(senales);
    }

    public boolean superaUmbral() {
        return probabilidad.compareTo(umbral) >= 0;
    }
}
