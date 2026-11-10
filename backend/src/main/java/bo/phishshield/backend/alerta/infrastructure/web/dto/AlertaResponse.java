package bo.phishshield.backend.alerta.infrastructure.web.dto;

import bo.phishshield.backend.alerta.domain.model.AlertaFila;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static bo.phishshield.backend.shared.infrastructure.web.dto.FechasLocales.enZonaLocal;

/** Una alerta de la bandeja. "id" es alertas.uuid_publico (V9). */
public record AlertaResponse(
        UUID id,
        UUID idAnalisis,
        String severidad,
        String estado,
        String url,
        String dominio,
        BigDecimal nivelRiesgo,
        String empresa,
        String usuario,
        String asignado,
        String notas,
        OffsetDateTime fechaNotificacion,
        OffsetDateTime fechaCierre
) {

    public static AlertaResponse desde(AlertaFila a) {
        return new AlertaResponse(
                a.id(), a.idAnalisis(), a.severidad(), a.estado(), a.url(), a.dominio(),
                a.nivelRiesgo(), a.empresa(), a.usuario(), a.asignado(), a.notas(),
                enZonaLocal(a.fechaNotificacion()), enZonaLocal(a.fechaCierre()));
    }
}
