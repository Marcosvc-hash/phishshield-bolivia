package bo.phishshield.backend.usuario.domain.model;

/**
 * El mismo mensaje para "email no registrado" y "contrasena incorrecta":
 * asi un atacante no puede averiguar que emails existen en la plataforma.
 */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Email o contrasena incorrectos");
    }
}
