package bo.phishshield.backend.qa.domain.port.out;

import bo.phishshield.backend.qa.domain.model.ArchivosReporte;
import bo.phishshield.backend.qa.domain.model.Ejecucion;

/**
 * Genera los dos reportes de una ejecucion terminada: el PDF detallado y
 * el HTML resumido. Si una captura de evidencia falta, el reporte se genera
 * igual y lo indica en ese paso.
 */
public interface GeneradorReportePort {

    ArchivosReporte generar(Ejecucion ejecucion);
}
