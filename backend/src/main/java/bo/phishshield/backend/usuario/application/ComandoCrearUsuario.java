package bo.phishshield.backend.usuario.application;

import bo.phishshield.backend.shared.domain.model.Rol;

import java.util.Locale;
import java.util.UUID;

/**
 * Entrada de CrearUsuarioAdminUseCase.
 *
 * @param empresa uuid publico de la empresa; null = sin empresa (o, para un
 *                ADMIN_EMPRESA, la suya)
 * @param ip      IP de la peticion, para la auditoria
 */
public record ComandoCrearUsuario(String nombres,
                                  String apellidos,
                                  String email,
                                  String contrasena,
                                  Rol rol,
                                  UUID empresa,
                                  String ip) {

    public ComandoCrearUsuario {
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
