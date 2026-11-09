package bo.phishshield.backend.usuario.domain.model;

/** El token de refresco no existe, vencio o la sesion ya fue cerrada. */
public class SesionInvalidaException extends RuntimeException {

    public SesionInvalidaException() {
        super("La sesion expiro o fue cerrada; inicie sesion de nuevo");
    }
}
