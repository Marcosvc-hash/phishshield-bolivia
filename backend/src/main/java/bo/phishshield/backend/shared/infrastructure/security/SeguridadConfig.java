package bo.phishshield.backend.shared.infrastructure.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Configuracion de Spring Security: quien puede llamar a cada ruta.
 *
 * - Sin sesion de servidor (STATELESS): cada peticion trae su JWT en el
 *   encabezado Authorization: Bearer ...
 * - CSRF desactivado: ese ataque usa cookies que el navegador manda solo,
 *   y esta API no usa cookies.
 * - El JWT se firma y se valida con la misma clave HMAC (HS256).
 */
@Configuration
@EnableConfigurationProperties(JwtPropiedades.class)
public class SeguridadConfig {

    public static final String EMISOR_JWT = "phishshield";

    @Bean
    public SecurityFilterChain cadenaDeSeguridad(
            HttpSecurity http,
            Converter<Jwt, AbstractAuthenticationToken> jwtAUsuario,
            RespuestasSeguridad respuestas) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rutas -> rutas
                        // Preflight de CORS y pagina de error de Spring
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()

                        // Autenticacion
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/registro", "/api/auth/login", "/api/auth/refresh").permitAll()

                        // El analizador sigue siendo publico: con token, el analisis
                        // queda a nombre del usuario; sin token, queda anonimo
                        .requestMatchers(HttpMethod.POST, "/api/analisis").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/analisis/*").permitAll()

                        // TEMPORAL: el frontend actual los llama sin sesion
                        .requestMatchers(HttpMethod.GET, "/api/analisis", "/api/empresas").permitAll()

                        // Administracion
                        .requestMatchers("/api/usuarios/**", "/api/empresas/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "ADMIN_EMPRESA")

                        .anyRequest().authenticated())
                .oauth2ResourceServer(rs -> rs
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAUsuario))
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas));

        return http.build();
    }

    @Bean
    public JwtEncoder jwtEncoder(JwtPropiedades propiedades) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(clave(propiedades)));
    }

    /** Ademas de la firma y el vencimiento, exige que el token lo haya emitido PhishShield. */
    @Bean
    public JwtDecoder jwtDecoder(JwtPropiedades propiedades) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(clave(propiedades))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(EMISOR_JWT));
        return decoder;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of("http://localhost:4200"));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", cors);
        return fuente;
    }

    private static SecretKey clave(JwtPropiedades propiedades) {
        return new SecretKeySpec(propiedades.secreto().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
