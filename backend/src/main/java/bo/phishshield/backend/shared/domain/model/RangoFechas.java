package bo.phishshield.backend.shared.domain.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Filtro "desde / hasta" de las paginas de administracion. Los dos extremos
 * son opcionales y se incluyen: hasta = 09/11 abarca todo el dia 9.
 * Los dias se cuentan en hora de Bolivia, igual que el panel.
 */
public record RangoFechas(LocalDate desde, LocalDate hasta) {

    public static final ZoneId ZONA = ZoneId.of("America/La_Paz");

    public RangoFechas {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
    }

    public static RangoFechas sinLimites() {
        return new RangoFechas(null, null);
    }

    /** Primer instante incluido (00:00 de 'desde' en La Paz), o null si no hay limite. */
    public OffsetDateTime inicio() {
        return desde == null ? null : desde.atStartOfDay(ZONA).toOffsetDateTime();
    }

    /** Primer instante que ya queda afuera (00:00 del dia siguiente a 'hasta'), o null. */
    public OffsetDateTime finExclusivo() {
        return hasta == null ? null : hasta.plusDays(1).atStartOfDay(ZONA).toOffsetDateTime();
    }
}
