package bo.phishshield.backend.historial.infrastructure.web.dto;

import bo.phishshield.backend.historial.domain.model.AnalisisFila;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static bo.phishshield.backend.shared.infrastructure.web.dto.FechasLocales.enZonaLocal;

public record AnalisisFilaResponse(
        UUID id,
        String url,
        String dominio,
        String resultado,
        BigDecimal nivelRiesgo,
        OffsetDateTime fecha,
        String usuario,
        String empresa,
        String estadoAlerta
) {

    public static AnalisisFilaResponse desde(AnalisisFila fila) {
        return new AnalisisFilaResponse(
                fila.id(),
                fila.url(),
                fila.dominio(),
                fila.resultado(),
                fila.nivelRiesgo(),
                enZonaLocal(fila.fecha()),
                fila.usuario(),
                fila.empresa(),
                fila.estadoAlerta());
    }
}
