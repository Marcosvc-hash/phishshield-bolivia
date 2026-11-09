package bo.phishshield.backend.usuario.application;

import bo.phishshield.backend.usuario.domain.model.EmailYaRegistradoException;
import bo.phishshield.backend.usuario.domain.model.PoliticaContrasena;
import bo.phishshield.backend.usuario.domain.model.Usuario;
import bo.phishshield.backend.usuario.domain.port.out.PasswordHasherPort;
import bo.phishshield.backend.usuario.domain.port.out.UsuarioRepositoryPort;

/**
 * Caso de uso: registro publico. Siempre crea un USUARIO sin empresa;
 * los roles administrativos los asigna un administrador.
 */
public class RegistrarUsuarioUseCase {

    private final UsuarioRepositoryPort usuarioRepository;
    private final PasswordHasherPort passwordHasher;

    public RegistrarUsuarioUseCase(UsuarioRepositoryPort usuarioRepository,
                                   PasswordHasherPort passwordHasher) {
        this.usuarioRepository = usuarioRepository;
        this.passwordHasher = passwordHasher;
    }

    public Usuario ejecutar(ComandoRegistrarUsuario comando) {
        PoliticaContrasena.validar(comando.contrasena());

        if (comando.email() != null && usuarioRepository.existeEmail(comando.email())) {
            throw new EmailYaRegistradoException(comando.email());
        }

        Usuario nuevo = Usuario.registrado(
                comando.nombres(),
                comando.apellidos(),
                comando.email(),
                passwordHasher.hashear(comando.contrasena()));

        return usuarioRepository.guardar(nuevo);
    }
}
