package bo.phishshield.backend.usuario.application;

import bo.phishshield.backend.usuario.domain.model.SesionInvalidaException;
import bo.phishshield.backend.usuario.domain.model.SesionRefresco;
import bo.phishshield.backend.usuario.domain.model.TokenRefresco;
import bo.phishshield.backend.usuario.domain.model.Usuario;
import bo.phishshield.backend.usuario.domain.port.out.SesionRepositoryPort;
import bo.phishshield.backend.usuario.domain.port.out.TokenAccesoPort;
import bo.phishshield.backend.usuario.domain.port.out.UsuarioRepositoryPort;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Caso de uso: cambiar un token de refresco por un token de acceso nuevo.
 *
 * El token de refresco se rota: el usado se revoca y se entrega otro.
 * Si alguien roba uno y lo usa, el dueno legitimo queda con un token
 * revocado y tiene que volver a iniciar sesion, lo que delata el robo.
 */
public class RefrescarSesionUseCase {

    private final UsuarioRepositoryPort usuarioRepository;
    private final TokenAccesoPort tokenAcceso;
    private final SesionRepositoryPort sesionRepository;
    private final Clock reloj;
    private final Duration duracionRefresco;

    public RefrescarSesionUseCase(UsuarioRepositoryPort usuarioRepository,
                                  TokenAccesoPort tokenAcceso,
                                  SesionRepositoryPort sesionRepository,
                                  Clock reloj,
                                  Duration duracionRefresco) {
        this.usuarioRepository = usuarioRepository;
        this.tokenAcceso = tokenAcceso;
        this.sesionRepository = sesionRepository;
        this.reloj = reloj;
        this.duracionRefresco = duracionRefresco;
    }

    public SesionIniciada ejecutar(String tokenRefresco) {
        if (tokenRefresco == null || tokenRefresco.isBlank()) {
            throw new SesionInvalidaException();
        }
        OffsetDateTime ahora = OffsetDateTime.now(reloj);

        SesionRefresco sesion = sesionRepository.buscarPorHash(TokenRefresco.hashDe(tokenRefresco))
                .filter(s -> s.vigente(ahora))
                .orElseThrow(SesionInvalidaException::new);

        // Si desactivaron al usuario mientras tenia la sesion abierta, no se renueva
        Usuario usuario = usuarioRepository.buscarPorId(sesion.idUsuario())
                .filter(Usuario::puedeIniciarSesion)
                .orElseThrow(SesionInvalidaException::new);

        sesionRepository.revocar(sesion.id());

        String nuevo = TokenRefresco.nuevo();
        OffsetDateTime expira = ahora.plus(duracionRefresco);
        sesionRepository.guardar(SesionRefresco.nueva(
                usuario.getId(), TokenRefresco.hashDe(nuevo), expira, sesion.userAgent()));

        return new SesionIniciada(usuario, tokenAcceso.emitir(usuario), nuevo, expira);
    }
}
