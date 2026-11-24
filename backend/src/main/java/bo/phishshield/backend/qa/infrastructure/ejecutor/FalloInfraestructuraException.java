package bo.phishshield.backend.qa.infrastructure.ejecutor;

/**
 * El caso no pudo comprobar nada porque algo no respondio o una
 * precondicion del entorno no se cumplio (servicio de IA caido, no se pudo
 * iniciar sesion). Queda FAIL con tipo de fallo INFRAESTRUCTURA: no dice si
 * PhishShield funciona bien o mal.
 */
public class FalloInfraestructuraException extends RuntimeException {

    public FalloInfraestructuraException(String mensaje) {
        super(mensaje);
    }

    public FalloInfraestructuraException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
