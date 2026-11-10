package bo.phishshield.backend.usuario.domain.port.out;

import bo.phishshield.backend.usuario.domain.model.SesionRefresco;

import java.util.Optional;

/** Persistencia de las sesiones (tokens de refresco) en la tabla sesiones. */
public interface SesionRepositoryPort {

    SesionRefresco guardar(SesionRefresco sesion);

    Optional<SesionRefresco> buscarPorHash(String hashToken);

    void revocar(Long idSesion);
}
