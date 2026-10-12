package bo.phishshield.backend.analisis.infrastructure.web.dto;

import bo.phishshield.backend.analisis.domain.model.Analisis;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Salida del endpoint. Expone el UUID publico, nunca el BIGINT interno.
 */
public record AnalisisResponse(
        UUID id,
        String url,
        String dominio,
        String tipo,
        String origen,
        String resultado,
        BigDecimal nivelRiesgo,
        boolean requiereAlerta,
        Integer tiempoMs,
        OffsetDateTime fechaAnalisis,
        List<IndicadorResponse> indicadores
) {

    /**
     * Zona en la que se muestran las fechas. El dominio y la base trabajan
     * con instantes; la conversion es un asunto de presentacion y vive aca.
     * Sin ella, un analisis nuevo salia con -04:00 y uno leido de la base en
     * UTC (Z): el mismo instante con dos formatos distintos.
     */
    public static final ZoneId ZONA = ZoneId.of("America/La_Paz");

    public static AnalisisResponse desde(Analisis analisis) {
        return new AnalisisResponse(
                analisis.getUuidPublico(),
                analisis.getContenido(),
                analisis.getDominio(),
                analisis.getTipo().aValorBd(),
                analisis.getOrigen().aValorBd(),
                analisis.getResultado().aValorBd(),
                analisis.getNivelRiesgo(),
                analisis.requiereAlerta(),
                analisis.getTiempoMs(),
                enZonaLocal(analisis.getFechaAnalisis()),
                analisis.getIndicadores().stream()
                        .map(IndicadorResponse::desde)
                        .toList());
    }

    private static OffsetDateTime enZonaLocal(OffsetDateTime fecha) {
        return fecha == null ? null : fecha.atZoneSameInstant(ZONA).toOffsetDateTime();
    }
}