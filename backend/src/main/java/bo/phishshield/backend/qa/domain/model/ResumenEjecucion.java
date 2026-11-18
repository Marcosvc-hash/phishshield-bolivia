package bo.phishshield.backend.qa.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/** Conteo de casos por estado y porcentaje de exito de una ejecucion. */
public record ResumenEjecucion(int total, int pass, int fail, int skipped, int notRun) {

    public ResumenEjecucion {
        if (total < 0 || pass < 0 || fail < 0 || skipped < 0 || notRun < 0) {
            throw new IllegalArgumentException("Los conteos no pueden ser negativos");
        }
        if (pass + fail + skipped + notRun != total) {
            throw new IllegalArgumentException("Los conteos por estado no suman el total");
        }
    }

    public static ResumenEjecucion de(Collection<EstadoCaso> estados) {
        int pass = 0, fail = 0, skipped = 0, notRun = 0;
        for (EstadoCaso estado : estados) {
            switch (estado) {
                case PASS -> pass++;
                case FAIL -> fail++;
                case SKIPPED -> skipped++;
                case NOT_RUN -> notRun++;
            }
        }
        return new ResumenEjecucion(estados.size(), pass, fail, skipped, notRun);
    }

    /** PASS sobre el total, con un decimal. 0 si no hay casos. */
    public BigDecimal porcentajeExito() {
        if (total == 0) {
            return BigDecimal.ZERO.setScale(1);
        }
        return BigDecimal.valueOf(pass * 100L)
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }

    /** Casos que ya tienen resultado (sirve para mostrar el avance "3 de 8"). */
    public int terminados() {
        return total - notRun;
    }
}
