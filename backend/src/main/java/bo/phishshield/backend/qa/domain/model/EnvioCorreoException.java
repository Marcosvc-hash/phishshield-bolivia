package bo.phishshield.backend.qa.domain.model;

/**
 * El servidor de correo rechazo o no recibio el mensaje. El mensaje de esta
 * excepcion se guarda en la ejecucion, asi que nunca debe incluir
 * contrasenas ni datos de la cuenta SMTP.
 */
public class EnvioCorreoException extends RuntimeException {

    public EnvioCorreoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
