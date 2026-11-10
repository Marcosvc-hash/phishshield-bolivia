package bo.phishshield.backend.usuario.infrastructure.security;

import bo.phishshield.backend.shared.infrastructure.security.JwtPropiedades;
import bo.phishshield.backend.shared.infrastructure.security.SeguridadConfig;
import bo.phishshield.backend.usuario.domain.model.TokenAcceso;
import bo.phishshield.backend.usuario.domain.model.Usuario;
import bo.phishshield.backend.usuario.domain.port.out.TokenAccesoPort;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Emite el JWT de acceso. Contenido (claims):
 *   iss = phishshield       quien lo emitio
 *   sub = uuid_publico      de quien es (nunca el BIGINT interno)
 *   rol = ADMIN | ...       solo informativo para el frontend
 *   iat / exp               emitido / vence
 *
 * El backend NO confia en el rol del token: en cada peticion vuelve a leer
 * el usuario de la base (JwtUsuarioActualConverter).
 */
@Component
public class JwtTokenAccesoAdapter implements TokenAccesoPort {

    public static final String CLAIM_ROL = "rol";

    private final JwtEncoder encoder;
    private final Duration duracion;

    public JwtTokenAccesoAdapter(JwtEncoder encoder, JwtPropiedades propiedades) {
        this.encoder = encoder;
        this.duracion = propiedades.duracionAcceso();
    }

    @Override
    public TokenAcceso emitir(Usuario usuario) {
        Instant ahora = Instant.now();
        Instant vence = ahora.plus(duracion);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(SeguridadConfig.EMISOR_JWT)
                .subject(usuario.getUuidPublico().toString())
                .issuedAt(ahora)
                .expiresAt(vence)
                .claim(CLAIM_ROL, usuario.getRol().name())
                .build();

        // Sin este encabezado Nimbus intentaria firmar con RS256 (clave publica/privada)
        JwsHeader encabezado = JwsHeader.with(MacAlgorithm.HS256).build();

        String valor = encoder.encode(JwtEncoderParameters.from(encabezado, claims)).getTokenValue();
        return new TokenAcceso(valor, OffsetDateTime.ofInstant(vence, ZoneOffset.UTC));
    }
}
