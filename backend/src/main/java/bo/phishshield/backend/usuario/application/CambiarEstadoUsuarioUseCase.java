package bo.phishshield.backend.usuario.application;

import bo.phishshield.backend.shared.domain.model.AccionAuditoria;
import bo.phishshield.backend.shared.domain.model.EventoAuditoria;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import bo.phishshield.backend.shared.domain.port.out.AuditoriaPort;
import bo.phishshield.backend.usuario.domain.model.EstadoUsuario;
import bo.phishshield.backend.usuario.domain.model.Usuario;
import bo.phishshield.backend.usuario.domain.model.UsuarioNoEncontradoException;
import bo.phishshield.backend.usuario.domain.port.out.UsuarioRepositoryPort;
import bo.phishshield.backend.usuario.domain.service.PoliticaGestionUsuarios;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Caso de uso: activar o desactivar una cuenta.
 *
 * No hace falta cerrar sus sesiones a mano: en cada peticion el backend
 * relee el estado del usuario (JwtUsuarioActualConverter) y el refresco
 * tambien lo revisa, asi que una cuenta desactivada queda afuera de inmediato.
 *
 * El cambio queda en auditoria (USUARIO_ACTIVADO / USUARIO_DESACTIVADO).
 */
public class CambiarEstadoUsuarioUseCase {

    private final UsuarioRepositoryPort usuarioRepository;
    private final AuditoriaPort auditoria;

    public CambiarEstadoUsuarioUseCase(UsuarioRepositoryPort usuarioRepository, AuditoriaPort auditoria) {
        this.usuarioRepository = usuarioRepository;
        this.auditoria = auditoria;
    }

    public Usuario ejecutar(UsuarioActual quien, UUID idObjetivo, boolean activar, String ip) {
        Usuario objetivo = usuarioRepository.buscarPorUuid(idObjetivo)
                .orElseThrow(() -> new UsuarioNoEncontradoException(idObjetivo));

        PoliticaGestionUsuarios.validarQuePuedeGestionar(quien, objetivo);

        EstadoUsuario anterior = objetivo.getEstado();
        if (activar) {
            objetivo.activar();
        } else {
            objetivo.desactivar();
        }
        Usuario guardado = usuarioRepository.guardar(objetivo);

        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("email", guardado.getEmail());
        detalle.put("estado_anterior", anterior.aValorBd());
        detalle.put("estado_nuevo", guardado.getEstado().aValorBd());
        auditoria.registrar(EventoAuditoria.de(
                activar ? AccionAuditoria.USUARIO_ACTIVADO : AccionAuditoria.USUARIO_DESACTIVADO,
                quien, "usuarios", guardado.getId(), detalle, ip));

        return guardado;
    }
}
