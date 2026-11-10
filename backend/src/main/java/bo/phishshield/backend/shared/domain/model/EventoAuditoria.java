package bo.phishshield.backend.shared.domain.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Una accion administrativa para dejar en la tabla auditoria.
 *
 * @param idUsuario          quien hizo la accion
 * @param entidad            tabla afectada (usuarios, alertas, dominios_lista, empresas)
 * @param idEntidadAfectada  BIGINT interno de la fila afectada; queda en la base,
 *                           no sale por la API
 * @param detalle            datos legibles del cambio; se guarda como JSONB
 * @param ip                 IP de quien hizo la peticion
 */
public record EventoAuditoria(AccionAuditoria accion,
                              Long idUsuario,
                              String entidad,
                              Long idEntidadAfectada,
                              Map<String, Object> detalle,
                              String ip) {

    public EventoAuditoria {
        if (accion == null) {
            throw new IllegalArgumentException("El evento de auditoria necesita una accion");
        }
        // LinkedHashMap y no Map.copyOf: conserva el orden y admite valores null
        detalle = detalle == null ? Map.of() : new LinkedHashMap<>(detalle);
    }

    public static EventoAuditoria de(AccionAuditoria accion, UsuarioActual quien, String entidad,
                                     Long idEntidadAfectada, Map<String, Object> detalle, String ip) {
        return new EventoAuditoria(accion, quien.idUsuario(), entidad, idEntidadAfectada, detalle, ip);
    }
}
