package bo.phishshield.backend.analisis.domain.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Calcula el hash que va en la columna contenido_hash CHAR(64). */
public final class Sha256 {

    private Sha256() {
        // clase de utilidad: no se instancia
    }

    public static String de(String texto) {
        if (texto == null) {
            throw new IllegalArgumentException("No se puede hashear un texto nulo");
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en esta JVM", e);
        }
    }
}