package bo.phishshield.backend.usuario.infrastructure.web.dto;

import bo.phishshield.backend.usuario.domain.model.Usuario;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Una fila de la gestion de usuarios. La empresa va por nombre: el BIGINT
 * interno nunca sale de la API.
 */
public record UsuarioAdminResponse(
        UUID id,
        String nombreCompleto,
        String email,
        String rol,
        String estado,
        String empresa,
        boolean emailVerificado,
        int intentosFallidos,
        OffsetDateTime ultimoLogin,
        OffsetDateTime creadoEn
) {

    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");

    public static UsuarioAdminResponse desde(Usuario usuario, String nombreEmpresa) {
        return new UsuarioAdminResponse(
                usuario.getUuidPublico(),
                usuario.nombreCompleto(),
                usuario.getEmail(),
                usuario.getRol().name(),
                usuario.getEstado().aValorBd(),
                nombreEmpresa,
                usuario.isEmailVerificado(),
                usuario.getIntentosFallidos(),
                enZonaLocal(usuario.getUltimoLogin()),
                enZonaLocal(usuario.getCreadoEn()));
    }

    private static OffsetDateTime enZonaLocal(OffsetDateTime fecha) {
        return fecha == null ? null : fecha.atZoneSameInstant(ZONA).toOffsetDateTime();
    }
}
