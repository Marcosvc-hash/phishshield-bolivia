package bo.phishshield.backend.qa.domain.model;

import java.math.BigDecimal;

/**
 * Una medicion de una prueba no funcional (tabla qa_metricas), por ejemplo
 * p95 = 840 ms con umbral 2000 ms.
 *
 * @param umbral  limite contra el que se compara; null si la metrica es solo informativa
 * @param cumple  si respeta el umbral; null cuando no hay umbral (chk_qa_metricas_umbral)
 * @param escalon numero de escalon en la prueba de estres; null en las demas
 */
public record MetricaRendimiento(
        String nombre,
        BigDecimal valor,
        String unidad,
        BigDecimal umbral,
        Boolean cumple,
        Integer escalon
) {

    public MetricaRendimiento {
        if (nombre == null || nombre.isBlank() || unidad == null || unidad.isBlank()) {
            throw new IllegalArgumentException("La métrica necesita nombre y unidad");
        }
        if (valor == null) {
            throw new IllegalArgumentException("La métrica " + nombre + " no tiene valor");
        }
        if (umbral == null && cumple != null) {
            throw new IllegalArgumentException("La métrica " + nombre + " no puede cumplir un umbral que no tiene");
        }
        if (umbral != null && cumple == null) {
            throw new IllegalArgumentException("La métrica " + nombre + " tiene umbral: hay que decir si lo cumple");
        }
        nombre = nombre.strip();
        unidad = unidad.strip();
    }

    /** Solo para mostrar (por ejemplo, el throughput). */
    public static MetricaRendimiento informativa(String nombre, BigDecimal valor, String unidad) {
        return new MetricaRendimiento(nombre, valor, unidad, null, null, null);
    }

    /** Cumple si el valor queda POR DEBAJO del umbral: tiempos, tasa de error. */
    public static MetricaRendimiento conMaximo(String nombre, BigDecimal valor, String unidad, BigDecimal umbral) {
        return new MetricaRendimiento(nombre, valor, unidad, umbral, valor.compareTo(umbral) < 0, null);
    }

    /** Cumple si el valor ALCANZA el umbral: precision, recall. */
    public static MetricaRendimiento conMinimo(String nombre, BigDecimal valor, String unidad, BigDecimal umbral) {
        return new MetricaRendimiento(nombre, valor, unidad, umbral, valor.compareTo(umbral) >= 0, null);
    }

    /** La misma metrica asignada a un escalon de la prueba de estres. */
    public MetricaRendimiento enEscalon(int numero) {
        if (numero < 1) {
            throw new IllegalArgumentException("Los escalones se numeran desde 1");
        }
        return new MetricaRendimiento(nombre, valor, unidad, umbral, cumple, numero);
    }

    public boolean incumple() {
        return Boolean.FALSE.equals(cumple);
    }
}
