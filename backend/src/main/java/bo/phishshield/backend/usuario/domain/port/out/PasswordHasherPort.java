package bo.phishshield.backend.usuario.domain.port.out;

/** Hashea y compara contrasenas. El dominio no sabe que detras hay BCrypt. */
public interface PasswordHasherPort {

    String hashear(String contrasenaPlana);

    boolean coincide(String contrasenaPlana, String hash);
}
