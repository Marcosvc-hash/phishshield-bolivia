package bo.phishshield.backend.usuario.infrastructure.web;

import bo.phishshield.backend.empresa.domain.model.Empresa;
import bo.phishshield.backend.empresa.domain.port.out.EmpresaRepositoryPort;
import bo.phishshield.backend.shared.domain.model.Rol;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import bo.phishshield.backend.shared.infrastructure.web.dto.CambiarEstadoRequest;
import bo.phishshield.backend.usuario.application.CambiarEstadoUsuarioUseCase;
import bo.phishshield.backend.usuario.application.CambiarRolUsuarioUseCase;
import bo.phishshield.backend.usuario.application.ComandoCrearUsuario;
import bo.phishshield.backend.usuario.application.CrearUsuarioAdminUseCase;
import bo.phishshield.backend.usuario.application.ListarUsuariosUseCase;
import bo.phishshield.backend.usuario.domain.model.Usuario;
import bo.phishshield.backend.usuario.infrastructure.web.dto.CambiarRolRequest;
import bo.phishshield.backend.usuario.infrastructure.web.dto.CrearUsuarioRequest;
import bo.phishshield.backend.usuario.infrastructure.web.dto.UsuarioAdminResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Gestion de usuarios para ADMIN (todos) y ADMIN_EMPRESA (los de su empresa).
 * Quien puede tocar a quien lo decide PoliticaGestionUsuarios, en el dominio.
 */
@RestController
@RequestMapping("/api/admin/usuarios")
public class AdminUsuarioController {

    private final ListarUsuariosUseCase listarUsuarios;
    private final CambiarEstadoUsuarioUseCase cambiarEstado;
    private final CrearUsuarioAdminUseCase crearUsuario;
    private final CambiarRolUsuarioUseCase cambiarRol;
    private final EmpresaRepositoryPort empresaRepository;

    public AdminUsuarioController(ListarUsuariosUseCase listarUsuarios,
                                  CambiarEstadoUsuarioUseCase cambiarEstado,
                                  CrearUsuarioAdminUseCase crearUsuario,
                                  CambiarRolUsuarioUseCase cambiarRol,
                                  EmpresaRepositoryPort empresaRepository) {
        this.listarUsuarios = listarUsuarios;
        this.cambiarEstado = cambiarEstado;
        this.crearUsuario = crearUsuario;
        this.cambiarRol = cambiarRol;
        this.empresaRepository = empresaRepository;
    }

    @GetMapping
    public List<UsuarioAdminResponse> listar(@AuthenticationPrincipal UsuarioActual quien) {
        Map<Long, String> empresas = nombresDeEmpresas();
        return listarUsuarios.ejecutar(quien).stream()
                .map(u -> UsuarioAdminResponse.desde(u, empresas.get(u.getIdEmpresa())))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioAdminResponse crear(@AuthenticationPrincipal UsuarioActual quien,
                                      @Valid @RequestBody CrearUsuarioRequest peticion,
                                      HttpServletRequest http) {
        Usuario creado = crearUsuario.ejecutar(quien, new ComandoCrearUsuario(
                peticion.nombres(),
                peticion.apellidos(),
                peticion.email(),
                peticion.contrasena(),
                Rol.desdeNombre(peticion.rol()),
                peticion.empresa(),
                http.getRemoteAddr()));
        return responder(creado);
    }

    /** Cuerpo: {"activo": true} para activar, {"activo": false} para desactivar. */
    @PatchMapping("/{id}/estado")
    public UsuarioAdminResponse cambiarEstado(@AuthenticationPrincipal UsuarioActual quien,
                                              @PathVariable UUID id,
                                              @Valid @RequestBody CambiarEstadoRequest peticion,
                                              HttpServletRequest http) {
        return responder(cambiarEstado.ejecutar(quien, id, peticion.activo(), http.getRemoteAddr()));
    }

    /** Cuerpo: {"rol": "ADMIN_EMPRESA"}. */
    @PatchMapping("/{id}/rol")
    public UsuarioAdminResponse cambiarRol(@AuthenticationPrincipal UsuarioActual quien,
                                           @PathVariable UUID id,
                                           @Valid @RequestBody CambiarRolRequest peticion,
                                           HttpServletRequest http) {
        return responder(cambiarRol.ejecutar(quien, id, Rol.desdeNombre(peticion.rol()), http.getRemoteAddr()));
    }

    private UsuarioAdminResponse responder(Usuario usuario) {
        return UsuarioAdminResponse.desde(usuario, nombresDeEmpresas().get(usuario.getIdEmpresa()));
    }

    /** Una sola consulta para todas las filas (hay pocas empresas). */
    private Map<Long, String> nombresDeEmpresas() {
        return empresaRepository.listarTodas().stream()
                .collect(Collectors.toMap(Empresa::getId, Empresa::getNombre));
    }
}
