package bo.phishshield.backend.analisis.domain.model;

public enum OrigenAnalisis {

    EXTENSION,
    PLUGIN_CORREO,
    WEB,
    API;

    public String aValorBd() {
        return name().toLowerCase();
    }

    public static OrigenAnalisis desdeValorBd(String valor) {
        if (valor == null) {
            throw new IllegalArgumentException("Origen nulo desde la base de datos");
        }
        return valueOf(valor.toUpperCase());
    }
}