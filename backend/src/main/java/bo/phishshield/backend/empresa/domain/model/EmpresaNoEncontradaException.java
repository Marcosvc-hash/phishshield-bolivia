package bo.phishshield.backend.empresa.domain.model;

import java.util.UUID;

public class EmpresaNoEncontradaException extends RuntimeException {

    public EmpresaNoEncontradaException(UUID id) {
        super("No se encontro la empresa " + id);
    }
}
