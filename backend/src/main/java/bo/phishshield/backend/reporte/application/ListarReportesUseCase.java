package bo.phishshield.backend.reporte.application;

import bo.phishshield.backend.reporte.domain.model.EmpresaDelReporte;
import bo.phishshield.backend.reporte.domain.model.ReporteEmpresa;
import bo.phishshield.backend.reporte.domain.model.ReporteNoEncontradoException;
import bo.phishshield.backend.reporte.domain.port.out.ReporteRepositoryPort;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.Pagina;
import bo.phishshield.backend.shared.domain.model.SolicitudPagina;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;

import java.util.UUID;

/**
 * Caso de uso: los reportes generados, del mas reciente al mas antiguo.
 * ADMIN ve todos (y puede filtrar por empresa); ADMIN_EMPRESA, los de la suya.
 */
public class ListarReportesUseCase {

    private final ReporteRepositoryPort reportes;

    public ListarReportesUseCase(ReporteRepositoryPort reportes) {
        this.reportes = reportes;
    }

    public Pagina<ReporteEmpresa> ejecutar(UsuarioActual quien, UUID idEmpresa, SolicitudPagina pagina) {
        AlcanceDatos alcance = AlcanceDatos.administrativo(quien);
        Long filtro = null;
        if (idEmpresa != null) {
            EmpresaDelReporte empresa = reportes.buscarEmpresaPorUuid(idEmpresa)
                    .filter(e -> alcance.incluye(e.id()))
                    .orElseThrow(() -> new ReporteNoEncontradoException("la empresa", idEmpresa));
            filtro = empresa.id();
        }
        return reportes.listar(alcance, filtro, pagina);
    }
}
