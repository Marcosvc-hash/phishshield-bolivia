package bo.phishshield.backend.usuario.domain.model;

import java.util.UUID;

public class UsuarioNoEncontradoException extends RuntimeException {

    public UsuarioNoEncontradoException(UUID id) {
        super("No existe el usuario " + id);
    }
}
