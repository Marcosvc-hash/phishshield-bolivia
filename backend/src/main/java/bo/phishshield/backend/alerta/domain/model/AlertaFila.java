package bo.phishshield.backend.alerta.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Una alerta con el contexto que se muestra en la bandeja.
 *
 * @param idAnalisis uuid del analisis que la origino (para abrirlo en el historial)
 * @param usuario    email de quien hizo el analisis; null si fue anonimo
 * @param asignado   email de quien la tomo en revision; null si nadie
 */
public record AlertaFila(UUID id,
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
                         OffsetDateTime fechaCierre) {
}
