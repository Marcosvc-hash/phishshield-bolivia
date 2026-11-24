package bo.phishshield.backend.reporte.infrastructure.web;

import bo.phishshield.backend.reporte.application.ComandoGenerarReporte;
import bo.phishshield.backend.reporte.application.DescargarReportePdfUseCase;
import bo.phishshield.backend.reporte.application.GenerarReporteUseCase;
import bo.phishshield.backend.reporte.application.ListarReportesUseCase;
import bo.phishshield.backend.reporte.infrastructure.web.dto.GenerarReporteRequest;
import bo.phishshield.backend.reporte.infrastructure.web.dto.ReporteResponse;
import bo.phishshield.backend.shared.domain.model.SolicitudPagina;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import bo.phishshield.backend.shared.infrastructure.web.dto.PaginaResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Reportes por empresa (HU-17). SeguridadConfig deja entrar a ADMIN y
 * ADMIN_EMPRESA en /api/admin/**; los casos de uso recortan por empresa.
 * Ejemplos: backend/reportes.http.
 */
@RestController
@RequestMapping("/api/admin/reportes")
public class ReporteController {

    private final GenerarReporteUseCase generar;
    private final ListarReportesUseCase listar;
    private final DescargarReportePdfUseCase descargar;

    public ReporteController(GenerarReporteUseCase generar, ListarReportesUseCase listar,
                             DescargarReportePdfUseCase descargar) {
        this.generar = generar;
        this.listar = listar;
        this.descargar = descargar;
    }

    /** 201: el reporte quedo guardado (si ya existia el mismo periodo, se recalculo y conserva su id). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReporteResponse generar(@AuthenticationPrincipal UsuarioActual quien,
                                   @Valid @RequestBody GenerarReporteRequest peticion,
                                   HttpServletRequest http) {
        return ReporteResponse.desde(generar.ejecutar(quien, new ComandoGenerarReporte(
                peticion.idEmpresa(), peticion.desde(), peticion.hasta(), http.getRemoteAddr())));
    }

    @GetMapping
    public PaginaResponse<ReporteResponse> listar(
            @AuthenticationPrincipal UsuarioActual quien,
            @RequestParam(required = false) UUID idEmpresa,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "" + SolicitudPagina.TAMANO_POR_DEFECTO) int tamano) {
        return PaginaResponse.desde(listar.ejecutar(quien, idEmpresa, new SolicitudPagina(pagina, tamano)),
                ReporteResponse::desde);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@AuthenticationPrincipal UsuarioActual quien, @PathVariable UUID id) {
        DescargarReportePdfUseCase.ReportePdf resultado = descargar.ejecutar(quien, id);
        String nombre = "reporte-" + resultado.reporte().periodo().desde() + "-al-"
                + resultado.reporte().periodo().hasta() + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build().toString())
                .body(resultado.pdf());
    }
}
