package bo.phishshield.backend.historial.infrastructure.web;

import bo.phishshield.backend.historial.application.ListarHistorialUseCase;
import bo.phishshield.backend.historial.application.ObtenerDetalleAnalisisUseCase;
import bo.phishshield.backend.historial.domain.model.FiltroHistorial;
import bo.phishshield.backend.historial.infrastructure.web.dto.AnalisisFilaResponse;
import bo.phishshield.backend.historial.infrastructure.web.dto.DetalleAnalisisResponse;
import bo.phishshield.backend.shared.domain.model.RangoFechas;
import bo.phishshield.backend.shared.domain.model.SolicitudPagina;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import bo.phishshield.backend.shared.infrastructure.web.dto.PaginaResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Historial de analisis para ADMIN y ADMIN_EMPRESA (SeguridadConfig deja
 * entrar a los dos en /api/admin/**; el caso de uso recorta por empresa).
 *
 * Ejemplo: GET /api/admin/analisis?resultado=phishing&desde=2026-11-01&q=bnb&pagina=0&tamano=20
 */
@RestController
@RequestMapping("/api/admin/analisis")
public class AdminAnalisisController {

    private final ListarHistorialUseCase listarHistorial;
    private final ObtenerDetalleAnalisisUseCase obtenerDetalle;

    public AdminAnalisisController(ListarHistorialUseCase listarHistorial,
                                   ObtenerDetalleAnalisisUseCase obtenerDetalle) {
        this.listarHistorial = listarHistorial;
        this.obtenerDetalle = obtenerDetalle;
    }

    @GetMapping
    public PaginaResponse<AnalisisFilaResponse> listar(
            @AuthenticationPrincipal UsuarioActual quien,
            @RequestParam(required = false) String resultado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "" + SolicitudPagina.TAMANO_POR_DEFECTO) int tamano) {

        FiltroHistorial filtro = new FiltroHistorial(
                FiltroHistorial.resultadoDesdeTexto(resultado),
                new RangoFechas(desde, hasta),
                q);

        return PaginaResponse.desde(
                listarHistorial.ejecutar(quien, filtro, new SolicitudPagina(pagina, tamano)),
                AnalisisFilaResponse::desde);
    }

    @GetMapping("/{id}")
    public DetalleAnalisisResponse detalle(@AuthenticationPrincipal UsuarioActual quien,
                                           @PathVariable UUID id) {
        return DetalleAnalisisResponse.desde(obtenerDetalle.ejecutar(quien, id));
    }
}
