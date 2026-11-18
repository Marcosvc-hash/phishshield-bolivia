package bo.phishshield.backend.qa.domain.model;

import java.util.UUID;

public class EjecucionNoEncontradaException extends RuntimeException {

    public EjecucionNoEncontradaException(UUID id) {
        super("No se encontró la ejecución " + id);
    }
}
