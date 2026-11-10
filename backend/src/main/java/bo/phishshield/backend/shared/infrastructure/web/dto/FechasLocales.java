package bo.phishshield.backend.shared.infrastructure.web.dto;

import bo.phishshield.backend.shared.domain.model.RangoFechas;

import java.time.OffsetDateTime;

/**
 * La base devuelve las fechas en UTC; la API las entrega con la hora de
 * Bolivia (-04:00) para que el frontend las muestre tal cual.
 */
public final class FechasLocales {

    private FechasLocales() {
        // clase de utilidad: no se instancia
    }

    public static OffsetDateTime enZonaLocal(OffsetDateTime fecha) {
        return fecha == null ? null : fecha.atZoneSameInstant(RangoFechas.ZONA).toOffsetDateTime();
    }
}
