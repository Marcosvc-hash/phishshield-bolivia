package bo.phishshield.backend.usuario.domain.model;

import java.nio.charset.StandardCharsets;

/**
 * Reglas de la contrasena en texto plano, antes de hashearla.
 * El maximo es de 72 bytes porque BCrypt ignora lo que sigue: dos
 * contrasenas que solo difieren despues del byte 72 serian iguales.
 */
public final class PoliticaContrasena {

    public static final int LARGO_MINIMO = 8;
    public static final int BYTES_MAXIMOS = 72;

    private PoliticaContrasena() {
        // clase de utilidad: no se instancia
    }

    public static void validar(String contrasena) {
        if (contrasena == null || contrasena.length() < LARGO_MINIMO) {
            throw new IllegalArgumentException(
                    "La contrasena debe tener al menos " + LARGO_MINIMO + " caracteres");
        }
        if (contrasena.getBytes(StandardCharsets.UTF_8).length > BYTES_MAXIMOS) {
            throw new IllegalArgumentException(
                    "La contrasena es demasiado larga (maximo " + BYTES_MAXIMOS + " bytes)");
        }
        if (contrasena.isBlank()) {
            throw new IllegalArgumentException("La contrasena no puede ser solo espacios");
        }
    }
}
