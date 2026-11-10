package bo.phishshield.backend.auditoria.domain.model;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Una fila del registro de accesos.
 *
 * @param usuario email de quien hizo la accion, o el email que se intento
 *                usar si no existe la cuenta; null si no hay ninguno
 * @param detalle el JSONB de la fila, ya convertido en mapa
 */
public record RegistroAuditoria(OffsetDateTime fecha,
                                String accion,
                                String usuario,
                                String entidad,
                                Map<String, Object> detalle,
                                String ip) {
}
