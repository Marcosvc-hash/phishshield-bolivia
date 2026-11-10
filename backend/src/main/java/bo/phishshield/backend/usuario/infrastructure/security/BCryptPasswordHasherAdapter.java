package bo.phishshield.backend.usuario.infrastructure.security;

import bo.phishshield.backend.usuario.domain.port.out.PasswordHasherPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * BCrypt con costo 10, el mismo de los hashes que ya estan en la base
 * ($2a$10$...). Cada hash lleva su propia sal aleatoria: la misma
 * contrasena produce un hash distinto cada vez.
 */
@Component
public class BCryptPasswordHasherAdapter implements PasswordHasherPort {

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(10);

    @Override
    public String hashear(String contrasenaPlana) {
        return bcrypt.encode(contrasenaPlana);
    }

    @Override
    public boolean coincide(String contrasenaPlana, String hash) {
        return bcrypt.matches(contrasenaPlana, hash);
    }
}
