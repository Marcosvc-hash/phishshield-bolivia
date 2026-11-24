package bo.phishshield.backend.reporte.application;

import bo.phishshield.backend.reporte.domain.model.ReporteEmpresa;
import bo.phishshield.backend.reporte.domain.model.ReporteNoEncontradoException;
import bo.phishshield.backend.reporte.domain.port.out.ReportePdfPort;
import bo.phishshield.backend.reporte.domain.port.out.ReporteRepositoryPort;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;

import java.util.UUID;

/**
 * Caso de uso: el PDF de un reporte guardado. Un reporte de otra empresa
 * responde 404 para el ADMIN_EMPRESA, igual que uno inexistente.
 */
public class DescargarReportePdfUseCase {

    /** El reporte y su PDF (el controlador usa los datos para el nombre del archivo). */
    public record ReportePdf(ReporteEmpresa reporte, byte[] pdf) {
    }

    private final ReporteRepositoryPort reportes;
    private final ReportePdfPort pdf;

    public DescargarReportePdfUseCase(ReporteRepositoryPort reportes, ReportePdfPort pdf) {
        this.reportes = reportes;
        this.pdf = pdf;
    }

    public ReportePdf ejecutar(UsuarioActual quien, UUID idReporte) {
        AlcanceDatos alcance = AlcanceDatos.administrativo(quien);
        ReporteEmpresa reporte = reportes.buscarPorUuid(idReporte)
                .filter(r -> alcance.incluye(r.empresa().id()))
                .orElseThrow(() -> new ReporteNoEncontradoException("el reporte", idReporte));
        return new ReportePdf(reporte, pdf.generar(reporte));
    }
}
