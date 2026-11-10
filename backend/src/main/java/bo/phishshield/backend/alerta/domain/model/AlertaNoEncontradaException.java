package bo.phishshield.backend.alerta.domain.model;

import java.util.UUID;

/**
 * La alerta no existe o es de otra empresa. Mismo 404 en los dos casos,
 * para no revelar que existe una alerta con ese id.
 */
public class AlertaNoEncontradaException extends RuntimeException {

    public AlertaNoEncontradaException(UUID id) {
        super("No se encontro la alerta " + id);
    }
}
