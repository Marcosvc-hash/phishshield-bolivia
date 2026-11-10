package bo.phishshield.backend.usuario.domain.port.out;

import bo.phishshield.backend.usuario.domain.model.EventoAcceso;

/** Deja constancia de cada intento de acceso (en la tabla auditoria). */
public interface RegistroAccesoPort {

    void registrar(EventoAcceso evento);
}
