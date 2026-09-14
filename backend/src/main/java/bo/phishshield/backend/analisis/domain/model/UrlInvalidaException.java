package bo.phishshield.backend.analisis.domain.model;

public class UrlInvalidaException extends RuntimeException {

    public UrlInvalidaException(String mensaje) {
        super(mensaje);
    }
}