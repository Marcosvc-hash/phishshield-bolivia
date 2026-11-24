package bo.phishshield.backend.reporte.domain.model;

import java.util.UUID;

/**
 * El reporte (o la empresa) no existe, o no entra en el alcance de quien
 * pregunta: un ADMIN_EMPRESA recibe el mismo 404 por un dato de otra
 * empresa que por uno inexistente.
 */
public class ReporteNoEncontradoException extends RuntimeException {

    public ReporteNoEncontradoException(String que, UUID id) {
        super("No se encontró " + que + " " + id);
    }
}
