package bo.phishshield.backend.usuario.application;

import bo.phishshield.backend.shared.domain.model.AccionAuditoria;
import bo.phishshield.backend.shared.domain.model.EventoAuditoria;
import bo.phishshield.backend.shared.domain.model.Rol;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import bo.phishshield.backend.shared.domain.port.out.AuditoriaPort;
import bo.phishshield.backend.usuario.domain.model.Usuario;
import bo.phishshield.backend.usuario.domain.model.UsuarioNoEncontradoException;
import bo.phishshield.backend.usuario.domain.port.out.UsuarioRepositoryPort;
import bo.phishshield.backend.usuario.domain.service.PoliticaGestionUsuarios;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Caso de uso: cambiar el rol de una cuenta. Las reglas estan en
 * PoliticaGestionUsuarios.validarCambioDeRol. El cambio rige en la
 * siguiente peticion del usuario, sin esperar a que venza su token.
 */
public class CambiarRolUsuarioUseCase {

    private final UsuarioRepositoryPort usuarioRepository;
    private final AuditoriaPort auditoria;

    public CambiarRolUsuarioUseCase(UsuarioRepositoryPort usuarioRepository, AuditoriaPort auditoria) {
        this.usuarioRepository = usuarioRepository;
        this.auditoria = auditoria;
    }

    public Usuario ejecutar(UsuarioActual quien, UUID idObjetivo, Rol nuevo, String ip) {
        Usuario objetivo = usuarioRepository.buscarPorUuid(idObjetivo)
                .orElseThrow(() -> new UsuarioNoEncontradoException(idObjetivo));

        PoliticaGestionUsuarios.validarCambioDeRol(quien, objetivo, nuevo);

        Rol anterior = objetivo.getRol();
        if (anterior == nuevo) {
            return objetivo;
        }
        objetivo.cambiarRol(nuevo);
        Usuario guardado = usuarioRepository.guardar(objetivo);

        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("email", guardado.getEmail());
        detalle.put("rol_anterior", anterior.name());
        detalle.put("rol_nuevo", nuevo.name());
        auditoria.registrar(EventoAuditoria.de(AccionAuditoria.USUARIO_ROL_CAMBIADO,
                quien, "usuarios", guardado.getId(), detalle, ip));

        return guardado;
    }
}
