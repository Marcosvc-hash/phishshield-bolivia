package bo.phishshield.backend.empresa.application;

import bo.phishshield.backend.empresa.domain.model.Empresa;
import bo.phishshield.backend.shared.domain.model.AccionAuditoria;
import bo.phishshield.backend.shared.domain.model.EventoAuditoria;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;

import java.util.LinkedHashMap;
import java.util.Map;

/** Arma el evento de auditoria de los casos de uso de empresa. Clase de paquete. */
final class AuditoriaEmpresa {

    private AuditoriaEmpresa() {
        // clase de utilidad: no se instancia
    }

    static EventoAuditoria evento(AccionAuditoria accion, UsuarioActual quien, Empresa empresa, String ip) {
        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("empresa", empresa.getNombre());
        detalle.put("nit", empresa.getNit());
        detalle.put("plan", empresa.getPlan());
        detalle.put("activa", empresa.isActiva());
        return EventoAuditoria.de(accion, quien, "empresas", empresa.getId(), detalle, ip);
    }
}
