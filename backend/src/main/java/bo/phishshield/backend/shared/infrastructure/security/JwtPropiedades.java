package bo.phishshield.backend.shared.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Lee el bloque phishshield.jwt de application.yaml.
 *
 * @param secreto          clave HMAC; viene de la variable de entorno JWT_SECRET
 * @param duracionAcceso   vida del JWT (corta: no se puede revocar)
 * @param duracionRefresco vida del token de refresco (se guarda en sesiones)
 */
@ConfigurationProperties(prefix = "phishshield.jwt")
public record JwtPropiedades(String secreto,
                             Duration duracionAcceso,
                             Duration duracionRefresco) {

    /** HS256 exige una clave de al menos 256 bits (32 bytes). */
    public static final int BYTES_MINIMOS_SECRETO = 32;

    public JwtPropiedades {
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < BYTES_MINIMOS_SECRETO) {
            throw new IllegalStateException(
                    "Falta la variable de entorno JWT_SECRET o tiene menos de "
                            + BYTES_MINIMOS_SECRETO + " caracteres");
        }
        if (duracionAcceso == null) {
            duracionAcceso = Duration.ofMinutes(15);
        }
        if (duracionRefresco == null) {
            duracionRefresco = Duration.ofDays(7);
        }
    }
}
