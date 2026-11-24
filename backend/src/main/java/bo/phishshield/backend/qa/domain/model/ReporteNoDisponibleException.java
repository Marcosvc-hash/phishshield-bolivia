package bo.phishshield.backend.qa.domain.model;

/** Se pidio un reporte o una evidencia que todavia no existe (por ejemplo, la ejecucion sigue en curso). */
public class ReporteNoDisponibleException extends RuntimeException {

    public ReporteNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
