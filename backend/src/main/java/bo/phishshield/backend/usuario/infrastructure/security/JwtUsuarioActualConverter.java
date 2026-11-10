package bo.phishshield.backend.usuario.infrastructure.security;

import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import bo.phishshield.backend.shared.infrastructure.security.UsuarioActualToken;
import bo.phishshield.backend.usuario.domain.model.Usuario;
import bo.phishshield.backend.usuario.domain.port.out.UsuarioRepositoryPort;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Se ejecuta en cada peticion con token, despues de que Spring valido la
 * firma y el vencimiento. Busca al usuario por el uuid del token y arma el
 * UsuarioActual con el rol y el estado que tiene HOY en la base.
 *
 * Asi, si un admin desactiva a alguien o le cambia el rol, el cambio rige
 * en la siguiente peticion, sin esperar a que venza el JWT.
 */
@Component
public class JwtUsuarioActualConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UsuarioRepositoryPort usuarioRepository;

    public JwtUsuarioActualConverter(UsuarioRepositoryPort usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UUID uuid;
        try {
            uuid = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new BadCredentialsException("El token no identifica a un usuario valido");
        }

        Usuario usuario = usuarioRepository.buscarPorUuid(uuid)
                .filter(Usuario::puedeIniciarSesion)
                .orElseThrow(() -> new DisabledException("La cuenta ya no esta habilitada"));

        return new UsuarioActualToken(
                new UsuarioActual(usuario.getId(), usuario.getUuidPublico(), usuario.getEmail(),
                        usuario.getIdEmpresa(), usuario.getRol()),
                jwt.getTokenValue());
    }
}
