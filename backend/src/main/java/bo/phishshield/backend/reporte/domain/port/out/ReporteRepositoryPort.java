package bo.phishshield.backend.reporte.domain.port.out;

import bo.phishshield.backend.reporte.domain.model.EmpresaDelReporte;
import bo.phishshield.backend.reporte.domain.model.PeriodoReporte;
import bo.phishshield.backend.reporte.domain.model.ReporteEmpresa;
import bo.phishshield.backend.reporte.domain.model.TotalesReporte;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.Pagina;
import bo.phishshield.backend.shared.domain.model.SolicitudPagina;

import java.util.Optional;
import java.util.UUID;

/** Datos de la vertical reporte: empresas, calculo de totales y la tabla reportes. */
public interface ReporteRepositoryPort {

    Optional<EmpresaDelReporte> buscarEmpresaPorUuid(UUID uuidPublico);

    Optional<EmpresaDelReporte> buscarEmpresaPorId(long idEmpresa);

    /** Cuenta, desde la base, los analisis, el phishing y las alertas de la empresa en el periodo. */
    TotalesReporte calcularTotales(long idEmpresa, PeriodoReporte periodo);

    /**
     * Guarda el reporte. Si ya habia uno de la misma empresa y periodo, le
     * pone los totales nuevos, quien lo genero y la fecha (conserva su id).
     *
     * @return el reporte guardado y si se creo (true) o se actualizo (false)
     */
    Guardado guardar(long idEmpresa, PeriodoReporte periodo, TotalesReporte totales, long idUsuario);

    Optional<ReporteEmpresa> buscarPorUuid(UUID uuidPublico);

    /** Del mas reciente al mas antiguo; filtrado por el alcance y, si viene, por una empresa. */
    Pagina<ReporteEmpresa> listar(AlcanceDatos alcance, Long idEmpresa, SolicitudPagina pagina);

    record Guardado(ReporteEmpresa reporte, boolean nuevo) {
    }
}
