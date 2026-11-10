package bo.phishshield.backend.alerta.domain.model;

/**
 * Otra persona cambio el estado de la alerta entre que se leyo y se quiso
 * guardar (dos administradores a la vez). La web responde 409 y el frontend
 * vuelve a cargar la alerta. Asi el segundo no pisa el cambio del primero.
 */
public class AlertaModificadaException extends RuntimeException {

    public AlertaModificadaException() {
        super("Otra persona cambio esta alerta mientras la editaba. Vuelva a cargarla e intente de nuevo.");
    }
}
