package bo.phishshield.backend.qa.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;

/**
 * Resumen de una prueba de carga: percentiles de latencia, throughput y
 * tasa de error. Los percentiles se calculan por rango mas cercano
 * (nearest-rank): p95 es la latencia que el 95 % de las peticiones no supera.
 *
 * @param peticiones total de peticiones enviadas
 * @param errores    peticiones sin respuesta o con un codigo distinto de 2xx
 * @param duracionMs tiempo total de la prueba
 */
public record EstadisticasLatencia(int peticiones, int errores, long duracionMs,
                                   long p50Ms, long p95Ms, long p99Ms, long mediaMs, long maximaMs) {

    public static EstadisticasLatencia de(Collection<Long> latenciasMs, int errores, long duracionMs) {
        long[] ordenadas = latenciasMs.stream().mapToLong(Long::longValue).sorted().toArray();
        if (ordenadas.length == 0) {
            return new EstadisticasLatencia(0, errores, duracionMs, 0, 0, 0, 0, 0);
        }
        return new EstadisticasLatencia(ordenadas.length, errores, duracionMs,
                percentil(ordenadas, 50), percentil(ordenadas, 95), percentil(ordenadas, 99),
                Math.round(Arrays.stream(ordenadas).average().orElse(0)), ordenadas[ordenadas.length - 1]);
    }

    /** Rango mas cercano: el valor en la posicion ceil(p/100 * n). */
    static long percentil(long[] ordenadas, int p) {
        int rango = (int) Math.ceil(p / 100.0 * ordenadas.length);
        return ordenadas[Math.max(0, Math.min(ordenadas.length - 1, rango - 1))];
    }

    /** Peticiones por segundo. */
    public BigDecimal throughput() {
        if (duracionMs <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(peticiones * 1000L).divide(BigDecimal.valueOf(duracionMs), 2, RoundingMode.HALF_UP);
    }

    /** Porcentaje de errores, con dos decimales. */
    public BigDecimal tasaErrorPct() {
        if (peticiones == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(errores * 100L).divide(BigDecimal.valueOf(peticiones), 2, RoundingMode.HALF_UP);
    }
}
