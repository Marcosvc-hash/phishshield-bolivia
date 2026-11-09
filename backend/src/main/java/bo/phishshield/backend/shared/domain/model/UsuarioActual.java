package bo.phishshield.backend.shared.domain.model;

import java.util.UUID;

/**
 * Quien hace la peticion. Lo arma la capa de seguridad a partir del token
 * y lo reciben los casos de uso, que asi no saben nada de JWT ni de Spring.
 */
public record UsuarioActual(Long idUsuario,
                            UUID uuidPublico,
                            String email,
                            Long idEmpresa,
                            Rol rol) {

    public UsuarioActual {
        if (idUsuario == null || uuidPublico == null || rol == null) {
            throw new IllegalArgumentException("El usuario actual necesita id, uuid y rol");
        }
    }

    public boolean esAdmin() {
        return rol == Rol.ADMIN;
    }
}
