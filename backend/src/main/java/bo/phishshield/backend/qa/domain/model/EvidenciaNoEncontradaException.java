package bo.phishshield.backend.qa.domain.model;

/** La captura pedida no existe, o la ruta no es una captura de esa ejecucion. */
public class EvidenciaNoEncontradaException extends RuntimeException {

    public EvidenciaNoEncontradaException(String ruta) {
        super("No se encontró la evidencia " + ruta);
    }
}
