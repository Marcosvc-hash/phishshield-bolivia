package bo.phishshield.backend.qa.domain.model;

/** La seleccion de casos no se puede ejecutar: vacia, de otra suite, repetida o con casos inactivos. */
public class SeleccionInvalidaException extends RuntimeException {

    public SeleccionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
