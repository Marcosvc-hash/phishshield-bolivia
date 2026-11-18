package bo.phishshield.backend.qa.domain.model;

/** El correo del destinatario esta vacio o no tiene un formato valido. */
public class CorreoInvalidoException extends RuntimeException {

    public CorreoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
