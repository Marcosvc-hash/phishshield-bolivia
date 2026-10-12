package bo.phishshield.backend.analisis.domain.model;

/**
 * Por donde se avisa al usuario. Coincide con chk_alertas_canal;
 * 'app' y 'webhook' existen en la base pero el motor todavia no los usa.
 */
public enum CanalAlerta {

    EMAIL,
    EXTENSION;

    /** Lo que llego por el plugin de correo se avisa por email; el resto, por la extension. */
    public static CanalAlerta desdeOrigen(OrigenAnalisis origen) {
        if (origen == null) {
            throw new IllegalArgumentException("El origen no puede ser nulo");
        }
        return origen == OrigenAnalisis.PLUGIN_CORREO ? EMAIL : EXTENSION;
    }

    public String aValorBd() {
        return name().toLowerCase();
    }
}
