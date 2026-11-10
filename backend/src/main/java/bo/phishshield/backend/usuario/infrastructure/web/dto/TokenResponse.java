package bo.phishshield.backend.usuario.infrastructure.web.dto;

import bo.phishshield.backend.usuario.application.SesionIniciada;

import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Salida del login y del refresco.
 * El frontend manda tokenAcceso en cada peticion (Authorization: Bearer ...)
 * y usa tokenRefresco solo para pedir un tokenAcceso nuevo cuando vence.
 */
public record TokenResponse(
        String tokenAcceso,
        String tipo,
        OffsetDateTime accesoExpiraEn,
        String tokenRefresco,
        OffsetDateTime refrescoExpiraEn,
        UsuarioSesionResponse usuario
) {

    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");

    public static TokenResponse desde(SesionIniciada sesion) {
        return new TokenResponse(
                sesion.tokenAcceso().valor(),
                "Bearer",
                sesion.tokenAcceso().expiraEn().atZoneSameInstant(ZONA).toOffsetDateTime(),
                sesion.tokenRefresco(),
                sesion.refrescoExpiraEn().atZoneSameInstant(ZONA).toOffsetDateTime(),
                UsuarioSesionResponse.desde(sesion.usuario()));
    }
}
