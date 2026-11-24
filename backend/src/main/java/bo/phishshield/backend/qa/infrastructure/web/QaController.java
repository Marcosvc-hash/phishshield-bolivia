package bo.phishshield.backend.qa.infrastructure.web;

import bo.phishshield.backend.qa.application.ComandoIniciarEjecucion;
import bo.phishshield.backend.qa.application.DetenerEjecucionUseCase;
import bo.phishshield.backend.qa.application.IniciarEjecucionUseCase;
import bo.phishshield.backend.qa.application.ListarCatalogoUseCase;
import bo.phishshield.backend.qa.application.ListarEjecucionesUseCase;
import bo.phishshield.backend.qa.application.ObtenerDetalleEjecucionUseCase;
import bo.phishshield.backend.qa.application.ObtenerReporteUseCase;
import bo.phishshield.backend.qa.domain.model.ArchivoEvidencia;
import bo.phishshield.backend.qa.domain.model.FormatoReporte;
import bo.phishshield.backend.qa.domain.model.Suite;
import bo.phishshield.backend.qa.infrastructure.web.dto.CasoResponse;
import bo.phishshield.backend.qa.infrastructure.web.dto.EjecucionResponse;
import bo.phishshield.backend.qa.infrastructure.web.dto.FilaEjecucionResponse;
import bo.phishshield.backend.qa.infrastructure.web.dto.IniciarEjecucionRequest;
import bo.phishshield.backend.shared.domain.model.SolicitudPagina;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import bo.phishshield.backend.shared.infrastructure.web.dto.PaginaResponse;
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

import java.util.List;
import java.util.UUID;

/**
 * Centro de Pruebas QA. Toda la ruta es solo para ADMIN (SeguridadConfig)
 * y los casos de uso lo vuelven a exigir con AlcanceDatos.soloPlataforma.
 * Ejemplos de uso: backend/qa.http.
 */
@RestController
@RequestMapping("/api/admin/qa")
public class QaController {

    private final ListarCatalogoUseCase listarCatalogo;
    private final IniciarEjecucionUseCase iniciarEjecucion;
    private final ListarEjecucionesUseCase listarEjecuciones;
    private final ObtenerDetalleEjecucionUseCase obtenerDetalle;
    private final DetenerEjecucionUseCase detenerEjecucion;
    private final ObtenerReporteUseCase obtenerReporte;

    public QaController(ListarCatalogoUseCase listarCatalogo,
                        IniciarEjecucionUseCase iniciarEjecucion,
                        ListarEjecucionesUseCase listarEjecuciones,
                        ObtenerDetalleEjecucionUseCase obtenerDetalle,
                        DetenerEjecucionUseCase detenerEjecucion,
                        ObtenerReporteUseCase obtenerReporte) {
        this.listarCatalogo = listarCatalogo;
        this.iniciarEjecucion = iniciarEjecucion;
        this.listarEjecuciones = listarEjecuciones;
        this.obtenerDetalle = obtenerDetalle;
        this.detenerEjecucion = detenerEjecucion;
        this.obtenerReporte = obtenerReporte;
    }

    @GetMapping("/casos")
    public List<CasoResponse> casos(@AuthenticationPrincipal UsuarioActual quien) {
        return listarCatalogo.ejecutar(quien).stream().map(CasoResponse::desde).toList();
    }

    /** 202 Accepted: la ejecucion queda programada; su avance se consulta con GET /ejecuciones/{id}. */
    @PostMapping("/ejecuciones")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public EjecucionResponse iniciar(@AuthenticationPrincipal UsuarioActual quien,
                                     @Valid @RequestBody IniciarEjecucionRequest peticion) {
        Suite suite = peticion.suite() == null || peticion.suite().isBlank()
                ? null : Suite.desdeValorBd(peticion.suite());
        return EjecucionResponse.desde(iniciarEjecucion.ejecutar(quien,
                new ComandoIniciarEjecucion(suite, peticion.codigos(), peticion.correo(), peticion.parametros())));
    }

    @GetMapping("/ejecuciones")
    public PaginaResponse<FilaEjecucionResponse> ejecuciones(
            @AuthenticationPrincipal UsuarioActual quien,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "" + SolicitudPagina.TAMANO_POR_DEFECTO) int tamano) {
        return PaginaResponse.desde(listarEjecuciones.ejecutar(quien, new SolicitudPagina(pagina, tamano)),
                FilaEjecucionResponse::desde);
    }

    @GetMapping("/ejecuciones/{id}")
    public EjecucionResponse detalle(@AuthenticationPrincipal UsuarioActual quien, @PathVariable UUID id) {
        return EjecucionResponse.desde(obtenerDetalle.ejecutar(quien, id));
    }

    @PostMapping("/ejecuciones/{id}/detener")
    public EjecucionResponse detener(@AuthenticationPrincipal UsuarioActual quien, @PathVariable UUID id) {
        return EjecucionResponse.desde(detenerEjecucion.ejecutar(quien, id));
    }

    /** Reporte detallado (PDF). 409 mientras la ejecucion sigue activa. */
    @GetMapping("/ejecuciones/{id}/reporte.pdf")
    public ResponseEntity<byte[]> reportePdf(@AuthenticationPrincipal UsuarioActual quien, @PathVariable UUID id) {
        return descarga(obtenerReporte.ejecutar(quien, id, FormatoReporte.PDF), id, "pdf");
    }

    /** Reporte resumido (HTML autocontenido). 409 mientras la ejecucion sigue activa. */
    @GetMapping("/ejecuciones/{id}/reporte.html")
    public ResponseEntity<byte[]> reporteHtml(@AuthenticationPrincipal UsuarioActual quien, @PathVariable UUID id) {
        return descarga(obtenerReporte.ejecutar(quien, id, FormatoReporte.HTML), id, "html");
    }

    private static ResponseEntity<byte[]> descarga(ArchivoEvidencia archivo, UUID id, String extension) {
        String nombre = "reporte-qa-" + id.toString().substring(0, 8) + "." + extension;
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.tipoMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nombre).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(archivo.contenido());
    }
}
