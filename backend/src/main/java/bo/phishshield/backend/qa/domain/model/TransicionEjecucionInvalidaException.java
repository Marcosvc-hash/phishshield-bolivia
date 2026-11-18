package bo.phishshield.backend.qa.domain.model;

/** Se intento un cambio que el ciclo de vida de la ejecucion no permite (ver EstadoEjecucion). */
public class TransicionEjecucionInvalidaException extends RuntimeException {

    public TransicionEjecucionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
