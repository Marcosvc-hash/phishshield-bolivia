package bo.phishshield.backend.shared.domain.port.out;

import bo.phishshield.backend.shared.domain.model.EventoAuditoria;

/**
 * Deja constancia de las acciones administrativas (tabla auditoria).
 * Lo usan los casos de uso de usuario, alerta, listas y empresa.
 */
public interface AuditoriaPort {

    void registrar(EventoAuditoria evento);
}
