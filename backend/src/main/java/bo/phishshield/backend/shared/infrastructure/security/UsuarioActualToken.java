package bo.phishshield.backend.shared.infrastructure.security;

import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/**
 * La autenticacion que queda en el SecurityContext despues de validar el JWT.
 * Su principal es el UsuarioActual del dominio, asi un controlador lo recibe
 * con @AuthenticationPrincipal UsuarioActual quien.
 *
 * La autoridad ROLE_X es la que evaluan hasRole("X") en SeguridadConfig.
 */
public class UsuarioActualToken extends AbstractAuthenticationToken {

    private final UsuarioActual usuario;
    private final String tokenValor;

    public UsuarioActualToken(UsuarioActual usuario, String tokenValor) {
        super(List.of(new SimpleGrantedAuthority("ROLE_" + usuario.rol().name())));
        this.usuario = usuario;
        this.tokenValor = tokenValor;
        setAuthenticated(true);
    }

    @Override
    public UsuarioActual getPrincipal() {
        return usuario;
    }

    @Override
    public String getCredentials() {
        return tokenValor;
    }

    @Override
    public String getName() {
        return usuario.uuidPublico().toString();
    }
}
