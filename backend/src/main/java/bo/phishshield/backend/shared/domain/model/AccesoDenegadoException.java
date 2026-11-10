package bo.phishshield.backend.shared.domain.model;

/** El usuario esta autenticado, pero su rol no le permite esta operacion (HTTP 403). */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
