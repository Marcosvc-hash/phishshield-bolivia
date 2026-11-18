package bo.phishshield.backend.qa.domain.model;

import java.util.UUID;

/** Ya hay una ejecucion pendiente o en curso: se ejecuta una a la vez. */
public class EjecucionEnCursoException extends RuntimeException {

    public EjecucionEnCursoException(UUID idActiva) {
        super("Ya hay una ejecución en curso" + (idActiva != null ? " (" + idActiva + ")" : "")
                + ". Espere a que termine para lanzar otra.");
    }
}
