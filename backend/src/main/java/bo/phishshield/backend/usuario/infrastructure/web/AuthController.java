package bo.phishshield.backend.usuario.infrastructure.web;

import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import bo.phishshield.backend.usuario.application.CerrarSesionUseCase;
import bo.phishshield.backend.usuario.application.ComandoIniciarSesion;
import bo.phishshield.backend.usuario.application.ComandoRegistrarUsuario;
import bo.phishshield.backend.usuario.application.IniciarSesionUseCase;
import bo.phishshield.backend.usuario.application.RefrescarSesionUseCase;
import bo.phishshield.backend.usuario.application.RegistrarUsuarioUseCase;
import bo.phishshield.backend.usuario.domain.port.out.UsuarioRepositoryPort;
import bo.phishshield.backend.usuario.infrastructure.web.dto.LoginRequest;
import bo.phishshield.backend.usuario.infrastructure.web.dto.RefrescoRequest;
import bo.phishshield.backend.usuario.infrastructure.web.dto.RegistroRequest;
import bo.phishshield.backend.usuario.infrastructure.web.dto.TokenResponse;
import bo.phishshield.backend.usuario.infrastructure.web.dto.UsuarioSesionResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registro, login, refresco y logout.
 * Las reglas de acceso de cada ruta estan en SeguridadConfig.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegistrarUsuarioUseCase registrarUsuario;
    private final IniciarSesionUseCase iniciarSesion;
    private final RefrescarSesionUseCase refrescarSesion;
    private final CerrarSesionUseCase cerrarSesion;
    private final UsuarioRepositoryPort usuarioRepository;

    public AuthController(RegistrarUsuarioUseCase registrarUsuario,
                          IniciarSesionUseCase iniciarSesion,
                          RefrescarSesionUseCase refrescarSesion,
                          CerrarSesionUseCase cerrarSesion,
                          UsuarioRepositoryPort usuarioRepository) {
        this.registrarUsuario = registrarUsuario;
        this.iniciarSesion = iniciarSesion;
        this.refrescarSesion = refrescarSesion;
        this.cerrarSesion = cerrarSesion;
        this.usuarioRepository = usuarioRepository;
    }

    /** Publico. No inicia sesion: despues del registro el frontend llama a /login. */
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioSesionResponse registrar(@Valid @RequestBody RegistroRequest peticion) {
        return UsuarioSesionResponse.desde(registrarUsuario.ejecutar(new ComandoRegistrarUsuario(
                peticion.nombres(),
                peticion.apellidos(),
                peticion.email(),
                peticion.contrasena())));
    }

    @PostMapping("/login")
    public TokenResponse iniciarSesion(@Valid @RequestBody LoginRequest peticion,
                                       HttpServletRequest http) {
        return TokenResponse.desde(iniciarSesion.ejecutar(new ComandoIniciarSesion(
                peticion.email(),
                peticion.contrasena(),
                http.getRemoteAddr(),
                http.getHeader(HttpHeaders.USER_AGENT))));
    }

    @PostMapping("/refresh")
    public TokenResponse refrescar(@Valid @RequestBody RefrescoRequest peticion) {
        return TokenResponse.desde(refrescarSesion.ejecutar(peticion.tokenRefresco()));
    }

    /** Requiere token de acceso; el cuerpo con el token de refresco es opcional. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cerrarSesion(@AuthenticationPrincipal UsuarioActual quien,
                             @RequestBody(required = false) RefrescoRequest peticion,
                             HttpServletRequest http) {
        cerrarSesion.ejecutar(
                quien,
                peticion == null ? null : peticion.tokenRefresco(),
                http.getRemoteAddr(),
                http.getHeader(HttpHeaders.USER_AGENT));
    }

    /** Quien soy: lo usa el frontend al recargar la pagina para validar el token guardado. */
    @GetMapping("/yo")
    public ResponseEntity<UsuarioSesionResponse> yo(@AuthenticationPrincipal UsuarioActual quien) {
        return usuarioRepository.buscarPorId(quien.idUsuario())
                .map(UsuarioSesionResponse::desde)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
