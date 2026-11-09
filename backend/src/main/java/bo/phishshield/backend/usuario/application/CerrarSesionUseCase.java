package bo.phishshield.backend.usuario.application;

import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import bo.phishshield.backend.usuario.domain.model.EventoAcceso;
import bo.phishshield.backend.usuario.domain.model.TipoEventoAcceso;
import bo.phishshield.backend.usuario.domain.model.TokenRefresco;
import bo.phishshield.backend.usuario.domain.port.out.RegistroAccesoPort;
import bo.phishshield.backend.usuario.domain.port.out.SesionRepositoryPort;

/**
 * Caso de uso: cerrar sesion revocando el token de refresco.
 *
 * El token de acceso (JWT) sigue siendo valido hasta que vence, como maximo
 * 15 minutos: un JWT no se puede "borrar" porque el servidor no lo guarda.
 * Por eso dura poco, y el frontend lo descarta al cerrar sesion.
 */
public class CerrarSesionUseCase {

    private final SesionRepositoryPort sesionRepository;
    private final RegistroAccesoPort registroAcceso;

    public CerrarSesionUseCase(SesionRepositoryPort sesionRepository,
                               RegistroAccesoPort registroAcceso) {
        this.sesionRepository = sesionRepository;
        this.registroAcceso = registroAcceso;
    }

    /** Es idempotente: cerrar una sesion ya cerrada no es un error. */
    public void ejecutar(UsuarioActual quien, String tokenRefresco, String ip, String userAgent) {
        if (tokenRefresco != null && !tokenRefresco.isBlank()) {
            sesionRepository.buscarPorHash(TokenRefresco.hashDe(tokenRefresco))
                    // Solo el dueno puede cerrar su propia sesion
                    .filter(s -> s.idUsuario().equals(quien.idUsuario()))
                    .filter(s -> !s.revocada())
                    .ifPresent(s -> sesionRepository.revocar(s.id()));
        }
        registroAcceso.registrar(new EventoAcceso(
                TipoEventoAcceso.LOGOUT, quien.idUsuario(), quien.email(), null, ip, userAgent));
    }
}
