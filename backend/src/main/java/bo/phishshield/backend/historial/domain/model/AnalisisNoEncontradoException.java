package bo.phishshield.backend.historial.domain.model;

import java.util.UUID;

/**
 * El analisis no existe o esta fuera del alcance de quien lo pide.
 * Las dos situaciones dan el mismo 404 a proposito: un admin de empresa
 * no debe poder averiguar si un id de otra empresa existe.
 */
public class AnalisisNoEncontradoException extends RuntimeException {

    public AnalisisNoEncontradoException(UUID id) {
        super("No se encontro el analisis " + id);
    }
}
