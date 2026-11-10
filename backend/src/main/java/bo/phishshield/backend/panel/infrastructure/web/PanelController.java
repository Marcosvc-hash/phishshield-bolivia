package bo.phishshield.backend.panel.infrastructure.web;

import bo.phishshield.backend.panel.application.ObtenerPanelUseCase;
import bo.phishshield.backend.panel.infrastructure.web.dto.PanelResponse;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Panel de administracion. SeguridadConfig ya deja entrar solo a ADMIN y
 * ADMIN_EMPRESA en /api/admin/**; el caso de uso decide que datos ve cada uno.
 */
@RestController
@RequestMapping("/api/admin/panel")
public class PanelController {

    private final ObtenerPanelUseCase obtenerPanel;

    public PanelController(ObtenerPanelUseCase obtenerPanel) {
        this.obtenerPanel = obtenerPanel;
    }

    @GetMapping
    public PanelResponse obtener(@AuthenticationPrincipal UsuarioActual quien,
                                 @RequestParam(defaultValue = "" + ObtenerPanelUseCase.DIAS_POR_DEFECTO) int dias) {
        return PanelResponse.desde(obtenerPanel.ejecutar(quien, dias));
    }
}
