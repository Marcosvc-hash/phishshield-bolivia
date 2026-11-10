package bo.phishshield.backend.usuario.application;

import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import bo.phishshield.backend.usuario.domain.model.Usuario;
import bo.phishshield.backend.usuario.domain.model.UsuarioNoEncontradoException;
import bo.phishshield.backend.usuario.domain.port.out.UsuarioRepositoryPort;
import bo.phishshield.backend.usuario.domain.service.PoliticaGestionUsuarios;

import java.util.UUID;

/**
 * Caso de uso: activar o desactivar una cuenta.
 *
 * No hace falta cerrar sus sesiones a mano: en cada peticion el backend
 * relee el estado del usuario (JwtUsuarioActualConverter) y el refresco
 * tambien lo revisa, asi que una cuenta desactivada queda afuera de inmediato.
 */
public class CambiarEstadoUsuarioUseCase {

    private final UsuarioRepositoryPort usuarioRepository;

    public CambiarEstadoUsuarioUseCase(UsuarioRepositoryPort usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario ejecutar(UsuarioActual quien, UUID idObjetivo, boolean activar) {
        Usuario objetivo = usuarioRepository.buscarPorUuid(idObjetivo)
                .orElseThrow(() -> new UsuarioNoEncontradoException(idObjetivo));

        PoliticaGestionUsuarios.validarQuePuedeGestionar(quien, objetivo);

        if (activar) {
            objetivo.activar();
        } else {
            objetivo.desactivar();
        }
        return usuarioRepository.guardar(objetivo);
    }
}
