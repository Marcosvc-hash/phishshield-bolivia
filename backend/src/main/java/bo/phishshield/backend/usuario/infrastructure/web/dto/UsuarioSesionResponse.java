package bo.phishshield.backend.usuario.infrastructure.web.dto;

import bo.phishshield.backend.usuario.domain.model.Usuario;

import java.util.UUID;

/** Datos minimos del usuario con sesion: lo que necesita el frontend para el menu. */
public record UsuarioSesionResponse(
        UUID id,
        String nombreCompleto,
        String email,
        String rol
) {
    public static UsuarioSesionResponse desde(Usuario usuario) {
        return new UsuarioSesionResponse(
                usuario.getUuidPublico(),
                usuario.nombreCompleto(),
                usuario.getEmail(),
                usuario.getRol().name());
    }
}
