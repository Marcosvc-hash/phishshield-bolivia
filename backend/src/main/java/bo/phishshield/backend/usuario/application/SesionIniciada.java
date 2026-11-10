package bo.phishshield.backend.usuario.application;

import bo.phishshield.backend.usuario.domain.model.TokenAcceso;
import bo.phishshield.backend.usuario.domain.model.Usuario;

import java.time.OffsetDateTime;

/**
 * Lo que devuelven el login y el refresco: un token de acceso corto (JWT)
 * y un token de refresco largo para pedir otro sin volver a escribir la contrasena.
 */
public record SesionIniciada(Usuario usuario,
                             TokenAcceso tokenAcceso,
                             String tokenRefresco,
                             OffsetDateTime refrescoExpiraEn) {
}
