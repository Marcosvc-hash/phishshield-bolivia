package bo.phishshield.backend.reporte.domain.model;

/** Las fechas del reporte faltan, estan invertidas, terminan en el futuro o abarcan mas de un año. */
public class PeriodoInvalidoException extends RuntimeException {

    public PeriodoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
