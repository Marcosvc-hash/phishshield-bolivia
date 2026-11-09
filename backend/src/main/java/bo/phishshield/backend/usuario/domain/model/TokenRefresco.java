package bo.phishshield.backend.usuario.domain.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * El token de refresco no es un JWT: es un valor aleatorio sin significado.
 * Al cliente se le entrega el valor; en la base se guarda su SHA-256.
 */
public final class TokenRefresco {

    private static final int BYTES_ALEATORIOS = 32;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private TokenRefresco() {
        // clase de utilidad: no se instancia
    }

    public static String nuevo() {
        byte[] bytes = new byte[BYTES_ALEATORIOS];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hashDe(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("El token de refresco es obligatorio");
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en esta JVM", e);
        }
    }
}
