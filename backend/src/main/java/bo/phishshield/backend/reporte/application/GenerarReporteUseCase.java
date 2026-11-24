package bo.phishshield.backend.reporte.application;

import bo.phishshield.backend.reporte.domain.model.EmpresaDelReporte;
import bo.phishshield.backend.reporte.domain.model.PeriodoReporte;
import bo.phishshield.backend.reporte.domain.model.ReporteEmpresa;
import bo.phishshield.backend.reporte.domain.model.ReporteNoEncontradoException;
import bo.phishshield.backend.reporte.domain.model.TotalesReporte;
import bo.phishshield.backend.reporte.domain.port.out.ReporteRepositoryPort;
import bo.phishshield.backend.shared.domain.model.AccionAuditoria;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.EventoAuditoria;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import bo.phishshield.backend.shared.domain.port.out.AuditoriaPort;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Caso de uso (HU-17): generar el reporte de una empresa para un periodo.
 *
 *  - ADMIN: el de cualquier empresa (la indica en el pedido).
 *  - ADMIN_EMPRESA: solo el de la suya; si pide otra, 404 (como si no existiera).
 *  - USUARIO: 403.
 *
 * Los totales se calculan desde la base en el momento. Si ya habia un
 * reporte de esa empresa y periodo, se recalcula (no se duplica). La accion
 * queda en la auditoria.
 */
public class GenerarReporteUseCase {

    private final ReporteRepositoryPort reportes;
    private final AuditoriaPort auditoria;
    private final Clock reloj;

    public GenerarReporteUseCase(ReporteRepositoryPort reportes, AuditoriaPort auditoria, Clock reloj) {
        this.reportes = reportes;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    public ReporteEmpresa ejecutar(UsuarioActual quien, ComandoGenerarReporte comando) {
        AlcanceDatos alcance = AlcanceDatos.administrativo(quien);
        PeriodoReporte periodo = PeriodoReporte.cerrado(comando.desde(), comando.hasta(),
                LocalDate.now(reloj.withZone(PeriodoReporte.ZONA)));

        EmpresaDelReporte empresa = empresaDelPedido(alcance, comando);
        TotalesReporte totales = reportes.calcularTotales(empresa.id(), periodo);
        ReporteRepositoryPort.Guardado guardado = reportes.guardar(empresa.id(), periodo, totales, quien.idUsuario());

        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("reporte", guardado.reporte().uuidPublico().toString());
        detalle.put("empresa", empresa.nombre());
        detalle.put("desde", periodo.desde().toString());
        detalle.put("hasta", periodo.hasta().toString());
        detalle.put("total_analisis", totales.analisis());
        detalle.put("total_phishing", totales.phishing());
        detalle.put("total_alertas", totales.alertas());
        detalle.put("nuevo", guardado.nuevo());
        auditoria.registrar(EventoAuditoria.de(AccionAuditoria.REPORTE_GENERADO, quien, "reportes",
                guardado.reporte().id(), detalle, comando.ip()));

        return guardado.reporte();
    }

    private EmpresaDelReporte empresaDelPedido(AlcanceDatos alcance, ComandoGenerarReporte comando) {
        if (alcance.esGlobal()) {
            if (comando.idEmpresa() == null) {
                throw new IllegalArgumentException("Elija la empresa del reporte");
            }
            return reportes.buscarEmpresaPorUuid(comando.idEmpresa())
                    .orElseThrow(() -> new ReporteNoEncontradoException("la empresa", comando.idEmpresa()));
        }
        // ADMIN_EMPRESA: siempre la suya. Otra empresa responde 404, igual que una inexistente.
        EmpresaDelReporte propia = reportes.buscarEmpresaPorId(alcance.idEmpresa())
                .orElseThrow(() -> new ReporteNoEncontradoException("la empresa", comando.idEmpresa()));
        if (comando.idEmpresa() != null && !comando.idEmpresa().equals(propia.uuidPublico())) {
            throw new ReporteNoEncontradoException("la empresa", comando.idEmpresa());
        }
        return propia;
    }
}
