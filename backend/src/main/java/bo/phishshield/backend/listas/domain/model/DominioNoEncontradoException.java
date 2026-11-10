package bo.phishshield.backend.listas.domain.model;

import java.util.UUID;

public class DominioNoEncontradoException extends RuntimeException {

    public DominioNoEncontradoException(UUID id) {
        super("No se encontro el dominio " + id);
    }
}
