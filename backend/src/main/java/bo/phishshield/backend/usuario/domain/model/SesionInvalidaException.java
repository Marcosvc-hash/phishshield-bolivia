package bo.phishshield.backend.usuario.domain.model;

/** El token de refresco no existe, vencio o la sesion ya fue cerrada. */
public class SesionInvalidaException extends RuntimeException {

    public SesionInvalidaException() {
        super("La sesión expiró o fue cerrada; inicie sesión de nuevo");
    }
}
