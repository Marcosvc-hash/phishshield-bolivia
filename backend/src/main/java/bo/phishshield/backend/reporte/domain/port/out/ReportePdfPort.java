package bo.phishshield.backend.reporte.domain.port.out;

import bo.phishshield.backend.reporte.domain.model.ReporteEmpresa;

/** Arma el PDF de un reporte de empresa. */
public interface ReportePdfPort {

    byte[] generar(ReporteEmpresa reporte);
}
