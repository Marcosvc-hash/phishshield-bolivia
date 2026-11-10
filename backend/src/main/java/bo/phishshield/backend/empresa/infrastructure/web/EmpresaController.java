package bo.phishshield.backend.empresa.infrastructure.web;

import bo.phishshield.backend.empresa.application.CambiarEstadoEmpresaUseCase;
import bo.phishshield.backend.empresa.application.ComandoCrearEmpresa;
import bo.phishshield.backend.empresa.application.CrearEmpresaUseCase;
import bo.phishshield.backend.empresa.application.EditarEmpresaUseCase;
import bo.phishshield.backend.empresa.domain.port.out.EmpresaRepositoryPort;
import bo.phishshield.backend.empresa.infrastructure.web.dto.EmpresaRequest;
import bo.phishshield.backend.empresa.infrastructure.web.dto.EmpresaResponse;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import bo.phishshield.backend.shared.infrastructure.web.dto.CambiarEstadoRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Empresas cliente. Toda la ruta /api/empresas/** es solo para ADMIN
 * (SeguridadConfig); los casos de uso lo vuelven a exigir.
 */
@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {

    private final CrearEmpresaUseCase crearEmpresa;
    private final EditarEmpresaUseCase editarEmpresa;
    private final CambiarEstadoEmpresaUseCase cambiarEstado;
    private final EmpresaRepositoryPort empresaRepository;

    public EmpresaController(CrearEmpresaUseCase crearEmpresa,
                             EditarEmpresaUseCase editarEmpresa,
                             CambiarEstadoEmpresaUseCase cambiarEstado,
                             EmpresaRepositoryPort empresaRepository) {
        this.crearEmpresa = crearEmpresa;
        this.editarEmpresa = editarEmpresa;
        this.cambiarEstado = cambiarEstado;
        this.empresaRepository = empresaRepository;
    }

    @GetMapping
    public List<EmpresaResponse> listar() {
        return empresaRepository.listarTodas()
                .stream()
                .map(EmpresaResponse::desde)
                .toList();
    }

    /** El "id" publico es el uuid_publico: el BIGINT interno nunca sale de la API. */
    @GetMapping("/{id}")
    public ResponseEntity<EmpresaResponse> obtener(@PathVariable UUID id) {
        return empresaRepository.buscarPorUuid(id)
                .map(EmpresaResponse::desde)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmpresaResponse crear(@AuthenticationPrincipal UsuarioActual quien,
                                 @Valid @RequestBody EmpresaRequest request,
                                 HttpServletRequest http) {
        return EmpresaResponse.desde(crearEmpresa.ejecutar(quien, comando(request), http.getRemoteAddr()));
    }

    @PutMapping("/{id}")
    public EmpresaResponse editar(@AuthenticationPrincipal UsuarioActual quien,
                                  @PathVariable UUID id,
                                  @Valid @RequestBody EmpresaRequest request,
                                  HttpServletRequest http) {
        return EmpresaResponse.desde(editarEmpresa.ejecutar(quien, id, comando(request), http.getRemoteAddr()));
    }

    /** Cuerpo: {"activo": true} o {"activo": false}. */
    @PatchMapping("/{id}/estado")
    public EmpresaResponse cambiarEstado(@AuthenticationPrincipal UsuarioActual quien,
                                         @PathVariable UUID id,
                                         @Valid @RequestBody CambiarEstadoRequest request,
                                         HttpServletRequest http) {
        return EmpresaResponse.desde(cambiarEstado.ejecutar(quien, id, request.activo(), http.getRemoteAddr()));
    }

    private static ComandoCrearEmpresa comando(EmpresaRequest request) {
        return new ComandoCrearEmpresa(
                request.nombre(),
                request.nit(),
                request.sector(),
                request.ciudad(),
                request.plan());
    }
}
