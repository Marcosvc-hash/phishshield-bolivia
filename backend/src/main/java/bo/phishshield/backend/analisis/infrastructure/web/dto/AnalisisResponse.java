package bo.phishshield.backend.analisis.infrastructure.web.dto;

import bo.phishshield.backend.analisis.domain.model.Analisis;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
                analisis.getFechaAnalisis(),
                analisis.getIndicadores().stream()
                        .map(IndicadorResponse::desde)
                        .toList());
    }
}