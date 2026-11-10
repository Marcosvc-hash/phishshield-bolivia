package bo.phishshield.backend.alerta.infrastructure.web;

import bo.phishshield.backend.alerta.application.ActualizarAlertaUseCase;
import bo.phishshield.backend.alerta.application.ComandoActualizarAlerta;
import bo.phishshield.backend.alerta.application.ListarAlertasUseCase;
import bo.phishshield.backend.alerta.domain.model.EstadoAlerta;
import bo.phishshield.backend.alerta.domain.model.FiltroAlertas;
import bo.phishshield.backend.alerta.infrastructure.web.dto.ActualizarAlertaRequest;
import bo.phishshield.backend.alerta.infrastructure.web.dto.AlertaResponse;
import bo.phishshield.backend.shared.domain.model.SolicitudPagina;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import bo.phishshield.backend.shared.infrastructure.web.dto.PaginaResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Bandeja de alertas para ADMIN y ADMIN_EMPRESA. El controlador solo
 * traduce HTTP a comandos: las transiciones se validan en el dominio.
 */
@RestController
@RequestMapping("/api/admin/alertas")
public class AdminAlertaController {

    private final ListarAlertasUseCase listarAlertas;
    private final ActualizarAlertaUseCase actualizarAlerta;

    public AdminAlertaController(ListarAlertasUseCase listarAlertas,
                                 ActualizarAlertaUseCase actualizarAlerta) {
        this.listarAlertas = listarAlertas;
        this.actualizarAlerta = actualizarAlerta;
    }

    @GetMapping
    public PaginaResponse<AlertaResponse> listar(
            @AuthenticationPrincipal UsuarioActual quien,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String severidad,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "" + SolicitudPagina.TAMANO_POR_DEFECTO) int tamano) {

        return PaginaResponse.desde(
                listarAlertas.ejecutar(quien, FiltroAlertas.desdeTexto(estado, severidad),
                        new SolicitudPagina(pagina, tamano)),
                AlertaResponse::desde);
    }

    /** Respuesta: {"cantidad": 3}. Lo consulta el menu lateral. */
    @GetMapping("/pendientes")
    public Map<String, Long> pendientes(@AuthenticationPrincipal UsuarioActual quien) {
        return Map.of("cantidad", listarAlertas.contarPendientes(quien));
    }

    @PatchMapping("/{id}")
    public AlertaResponse actualizar(@AuthenticationPrincipal UsuarioActual quien,
                                     @PathVariable UUID id,
                                     @Valid @RequestBody ActualizarAlertaRequest peticion,
                                     HttpServletRequest http) {
        EstadoAlerta estado = peticion.estado() == null || peticion.estado().isBlank()
                ? null
                : EstadoAlerta.desdeValorBd(peticion.estado());

        return AlertaResponse.desde(actualizarAlerta.ejecutar(quien,
                new ComandoActualizarAlerta(id, estado, peticion.notas(), http.getRemoteAddr())));
    }
}
