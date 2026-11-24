package bo.phishshield.backend.reporte.infrastructure.config;

import bo.phishshield.backend.reporte.application.DescargarReportePdfUseCase;
import bo.phishshield.backend.reporte.application.GenerarReporteUseCase;
import bo.phishshield.backend.reporte.application.ListarReportesUseCase;
import bo.phishshield.backend.reporte.domain.port.out.ReportePdfPort;
import bo.phishshield.backend.reporte.domain.port.out.ReporteRepositoryPort;
import bo.phishshield.backend.reporte.infrastructure.pdf.ReporteEmpresaPdfAdapter;
import bo.phishshield.backend.shared.domain.port.out.AuditoriaPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Cablea los casos de uso de reportes por empresa (HU-17) sin anotar la capa application. */
@Configuration
public class ReporteConfig {

    private static final Clock RELOJ = Clock.systemDefaultZone();

    @Bean
    public ReportePdfPort reportePdf() {
        return new ReporteEmpresaPdfAdapter();
    }

    @Bean
    public GenerarReporteUseCase generarReporteUseCase(ReporteRepositoryPort reportes, AuditoriaPort auditoria) {
        return new GenerarReporteUseCase(reportes, auditoria, RELOJ);
    }

    @Bean
    public ListarReportesUseCase listarReportesUseCase(ReporteRepositoryPort reportes) {
        return new ListarReportesUseCase(reportes);
    }

    @Bean
    public DescargarReportePdfUseCase descargarReportePdfUseCase(ReporteRepositoryPort reportes, ReportePdfPort pdf) {
        return new DescargarReportePdfUseCase(reportes, pdf);
    }
}
