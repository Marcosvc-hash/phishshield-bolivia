package bo.phishshield.backend.usuario.application;

import java.util.Locale;

/**
 * @param ip        direccion de origen, para el registro de accesos
 * @param userAgent navegador o cliente, para el registro de accesos
 */
public record ComandoIniciarSesion(String email,
                                   String contrasena,
                                   String ip,
                                   String userAgent) {

    public ComandoIniciarSesion {
        if (email == null || email.isBlank() || contrasena == null || contrasena.isEmpty()) {
            throw new IllegalArgumentException("Email y contrasena son obligatorios");
        }
        email = email.trim().toLowerCase(Locale.ROOT);
    }
}
