package bo.phishshield.backend.auditoria.infrastructure.web.dto;

import bo.phishshield.backend.auditoria.domain.model.RegistroAuditoria;

import java.time.OffsetDateTime;
import java.util.Map;

import static bo.phishshield.backend.shared.infrastructure.web.dto.FechasLocales.enZonaLocal;

/** Una fila del registro. id_evento e id_entidad_afectada (BIGINT) no salen. */
public record RegistroAuditoriaResponse(
        OffsetDateTime fecha,
        String accion,
        String usuario,
        String entidad,
        Map<String, Object> detalle,
        String ip
) {

    public static RegistroAuditoriaResponse desde(RegistroAuditoria r) {
        return new RegistroAuditoriaResponse(
                enZonaLocal(r.fecha()), r.accion(), r.usuario(), r.entidad(), r.detalle(), r.ip());
    }
}
