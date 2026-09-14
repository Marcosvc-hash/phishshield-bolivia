package bo.phishshield.backend.analisis.domain.model;

public enum TipoAnalisis {

    URL,
    CORREO,
    IMAGEN;

    public String aValorBd() {
        return name().toLowerCase();
    }

    public static TipoAnalisis desdeValorBd(String valor) {
        if (valor == null) {
            throw new IllegalArgumentException("Tipo nulo desde la base de datos");
        }
        return valueOf(valor.toUpperCase());
    }
}