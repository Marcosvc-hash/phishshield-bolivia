package bo.phishshield.backend.qa.domain.model;

/** El asistente de IA no esta configurado o no respondio. Las pruebas funcionan igual sin el. */
public class AsistenteNoDisponibleException extends RuntimeException {

    public AsistenteNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
