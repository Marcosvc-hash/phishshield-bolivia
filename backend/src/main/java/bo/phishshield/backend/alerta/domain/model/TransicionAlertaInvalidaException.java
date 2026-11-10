package bo.phishshield.backend.alerta.domain.model;

/**
 * Se intento un cambio que el ciclo de vida no permite (por ejemplo,
 * cerrar una alerta pendiente o tocar una ya cerrada). La web responde 409.
 */
public class TransicionAlertaInvalidaException extends RuntimeException {

    public TransicionAlertaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
