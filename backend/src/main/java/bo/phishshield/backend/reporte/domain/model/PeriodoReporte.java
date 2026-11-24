package bo.phishshield.backend.reporte.domain.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * Periodo de un reporte: del dia "desde" al dia "hasta", ambos incluidos,
 * contados en hora de Bolivia (igual que el panel y el historial).
 *
 * Reglas: las dos fechas son obligatorias, "hasta" no puede ser anterior a
 * "desde" ni posterior a hoy, y el periodo no pasa de un año (366 dias).
 */
public record PeriodoReporte(LocalDate desde, LocalDate hasta) {

    public static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    public static final int MAXIMO_DIAS = 366;

    public PeriodoReporte {
        if (desde == null || hasta == null) {
            throw new PeriodoInvalidoException("Indique las fechas 'desde' y 'hasta' del reporte");
        }
        if (hasta.isBefore(desde)) {
            throw new PeriodoInvalidoException("La fecha 'hasta' no puede ser anterior a 'desde'");
        }
        if (dias(desde, hasta) > MAXIMO_DIAS) {
            throw new PeriodoInvalidoException("El período no puede pasar de " + MAXIMO_DIAS + " días");
        }
    }

    /** Valida ademas que el periodo no termine en el futuro (segun la fecha de hoy en Bolivia). */
    public static PeriodoReporte cerrado(LocalDate desde, LocalDate hasta, LocalDate hoy) {
        PeriodoReporte periodo = new PeriodoReporte(desde, hasta);
        if (hoy != null && hasta.isAfter(hoy)) {
            throw new PeriodoInvalidoException("El período no puede terminar después de hoy (" + hoy + ")");
        }
        return periodo;
    }

    /** Cantidad de dias del periodo, contando los dos extremos. */
    public long dias() {
        return dias(desde, hasta);
    }

    /** Primer instante incluido: 00:00 de "desde" en La Paz. */
    public OffsetDateTime inicio() {
        return desde.atStartOfDay(ZONA).toOffsetDateTime();
    }

    /** Primer instante que ya queda afuera: 00:00 del dia siguiente a "hasta". */
    public OffsetDateTime finExclusivo() {
        return hasta.plusDays(1).atStartOfDay(ZONA).toOffsetDateTime();
    }

    private static long dias(LocalDate desde, LocalDate hasta) {
        return ChronoUnit.DAYS.between(desde, hasta) + 1;
    }
}
