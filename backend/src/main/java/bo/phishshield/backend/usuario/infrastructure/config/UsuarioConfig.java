package bo.phishshield.backend.usuario.infrastructure.config;

import bo.phishshield.backend.shared.infrastructure.security.JwtPropiedades;
import bo.phishshield.backend.usuario.application.CambiarEstadoUsuarioUseCase;
import bo.phishshield.backend.usuario.application.CerrarSesionUseCase;
import bo.phishshield.backend.usuario.application.IniciarSesionUseCase;
import bo.phishshield.backend.usuario.application.ListarUsuariosUseCase;
import bo.phishshield.backend.usuario.application.RefrescarSesionUseCase;
import bo.phishshield.backend.usuario.application.RegistrarUsuarioUseCase;
import bo.phishshield.backend.usuario.domain.port.out.PasswordHasherPort;
import bo.phishshield.backend.usuario.domain.port.out.RegistroAccesoPort;
import bo.phishshield.backend.usuario.domain.port.out.SesionRepositoryPort;
import bo.phishshield.backend.usuario.domain.port.out.TokenAccesoPort;
import bo.phishshield.backend.usuario.domain.port.out.UsuarioRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Cablea los casos de uso de usuario y autenticacion sin anotar la capa application.
 */
@Configuration
public class UsuarioConfig {

    @Bean
    public RegistrarUsuarioUseCase registrarUsuarioUseCase(UsuarioRepositoryPort usuarioRepository,
                                                           PasswordHasherPort passwordHasher) {
        return new RegistrarUsuarioUseCase(usuarioRepository, passwordHasher);
    }

    @Bean
    public IniciarSesionUseCase iniciarSesionUseCase(UsuarioRepositoryPort usuarioRepository,
                                                     PasswordHasherPort passwordHasher,
                                                     TokenAccesoPort tokenAcceso,
                                                     SesionRepositoryPort sesionRepository,
                                                     RegistroAccesoPort registroAcceso,
                                                     JwtPropiedades jwt) {
        return new IniciarSesionUseCase(usuarioRepository, passwordHasher, tokenAcceso,
                sesionRepository, registroAcceso, Clock.systemDefaultZone(), jwt.duracionRefresco());
    }

    @Bean
    public RefrescarSesionUseCase refrescarSesionUseCase(UsuarioRepositoryPort usuarioRepository,
                                                         TokenAccesoPort tokenAcceso,
                                                         SesionRepositoryPort sesionRepository,
                                                         JwtPropiedades jwt) {
        return new RefrescarSesionUseCase(usuarioRepository, tokenAcceso, sesionRepository,
                Clock.systemDefaultZone(), jwt.duracionRefresco());
    }

    @Bean
    public CerrarSesionUseCase cerrarSesionUseCase(SesionRepositoryPort sesionRepository,
                                                   RegistroAccesoPort registroAcceso) {
        return new CerrarSesionUseCase(sesionRepository, registroAcceso);
    }

    @Bean
    public ListarUsuariosUseCase listarUsuariosUseCase(UsuarioRepositoryPort usuarioRepository) {
        return new ListarUsuariosUseCase(usuarioRepository);
    }

    @Bean
    public CambiarEstadoUsuarioUseCase cambiarEstadoUsuarioUseCase(UsuarioRepositoryPort usuarioRepository) {
        return new CambiarEstadoUsuarioUseCase(usuarioRepository);
    }
}
