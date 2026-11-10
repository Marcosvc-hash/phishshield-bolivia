package bo.phishshield.backend.auditoria.infrastructure.web;

import bo.phishshield.backend.auditoria.application.ListarAuditoriaUseCase;
import bo.phishshield.backend.auditoria.domain.model.FiltroAuditoria;
import bo.phishshield.backend.auditoria.infrastructure.web.dto.RegistroAuditoriaResponse;
import bo.phishshield.backend.shared.domain.model.RangoFechas;
import bo.phishshield.backend.shared.domain.model.SolicitudPagina;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import bo.phishshield.backend.shared.infrastructure.web.dto.PaginaResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Registro de accesos y acciones administrativas. Solo ADMIN: lo exige
 * SeguridadConfig por ruta y, otra vez, el caso de uso.
 */
@RestController
@RequestMapping("/api/admin/auditoria")
public class AuditoriaController {

    private final ListarAuditoriaUseCase listarAuditoria;

    public AuditoriaController(ListarAuditoriaUseCase listarAuditoria) {
        this.listarAuditoria = listarAuditoria;
    }

    @GetMapping
    public PaginaResponse<RegistroAuditoriaResponse> listar(
            @AuthenticationPrincipal UsuarioActual quien,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "" + SolicitudPagina.TAMANO_POR_DEFECTO) int tamano) {

        FiltroAuditoria filtro = new FiltroAuditoria(accion, usuario, new RangoFechas(desde, hasta));
        return PaginaResponse.desde(
                listarAuditoria.ejecutar(quien, filtro, new SolicitudPagina(pagina, tamano)),
                RegistroAuditoriaResponse::desde);
    }

    @GetMapping("/acciones")
    public List<String> acciones(@AuthenticationPrincipal UsuarioActual quien) {
        return listarAuditoria.acciones(quien);
    }
}
