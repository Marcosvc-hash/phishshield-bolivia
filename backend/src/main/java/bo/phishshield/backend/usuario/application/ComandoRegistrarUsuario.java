package bo.phishshield.backend.usuario.application;

import java.util.Locale;

public record ComandoRegistrarUsuario(String nombres,
                                      String apellidos,
                                      String email,
                                      String contrasena) {

    public ComandoRegistrarUsuario {
        // La base guarda los emails en minusculas (chk_usuarios_email)
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
        if (nombres != null) {
            nombres = nombres.trim();
        }
        if (apellidos != null) {
            apellidos = apellidos.isBlank() ? null : apellidos.trim();
        }
    }
}
