package bo.phishshield.backend.usuario.infrastructure.web.dto;

import bo.phishshield.backend.usuario.domain.model.Usuario;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UsuarioResponse(
        UUID id,
        Long idEmpresa,
        String nombreCompleto,
        String email,
        String rol,
        String estado,
        boolean emailVerificado,
        boolean puedeIniciarSesion,
        OffsetDateTime ultimoLogin,
        OffsetDateTime creadoEn
) {
    public static UsuarioResponse desde(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getUuidPublico(),
                usuario.getIdEmpresa(),
                usuario.nombreCompleto(),
                usuario.getEmail(),
                usuario.getRol(),
                usuario.getEstado(),
                usuario.isEmailVerificado(),
                usuario.puedeIniciarSesion(),
                usuario.getUltimoLogin(),
                usuario.getCreadoEn()
        );
    }
}