package bo.phishshield.backend.usuario.domain.model;

public enum EstadoUsuario {

    ACTIVO,
    INACTIVO,
    BLOQUEADO,
    PENDIENTE;

    public String aValorBd() {
        return name().toLowerCase();
    }

    public static EstadoUsuario desdeValorBd(String valor) {
        if (valor == null) {
            throw new IllegalArgumentException("Estado de usuario nulo desde la base de datos");
        }
        return valueOf(valor.toUpperCase());
    }
}
