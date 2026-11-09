package bo.phishshield.backend.shared.domain.model;

/**
 * Roles de la plataforma. La tabla usuarios guarda otros nombres
 * (chk_usuarios_rol), asi que se traducen aca sin tocar el esquema:
 *
 *   admin   -> ADMIN          (administra toda la plataforma)
 *   empresa -> ADMIN_EMPRESA  (administra los datos de su empresa)
 *   usuario -> USUARIO
 *
 * El valor 'analista' existe en el CHECK pero ningun usuario lo usa.
 */
public enum Rol {

    ADMIN("admin"),
    ADMIN_EMPRESA("empresa"),
    USUARIO("usuario");

    private final String valorBd;

    Rol(String valorBd) {
        this.valorBd = valorBd;
    }

    public String aValorBd() {
        return valorBd;
    }

    public static Rol desdeValorBd(String valor) {
        for (Rol rol : values()) {
            if (rol.valorBd.equals(valor)) {
                return rol;
            }
        }
        throw new IllegalArgumentException("Rol desconocido en la base de datos: " + valor);
    }
}
