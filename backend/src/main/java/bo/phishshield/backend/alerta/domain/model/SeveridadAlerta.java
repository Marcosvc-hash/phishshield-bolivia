package bo.phishshield.backend.alerta.domain.model;

/**
 * Severidades de chk_alertas_severidad. A diferencia de
 * analisis.domain.model.Severidad (la que genera el motor), aca esta
 * tambien 'baja', porque la bandeja debe poder leer cualquier fila.
 */
public enum SeveridadAlerta {

    BAJA,
    MEDIA,
    ALTA,
    CRITICA;

    public String aValorBd() {
        return name().toLowerCase();
    }

    public static SeveridadAlerta desdeValorBd(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("La severidad es obligatoria");
        }
        try {
            return valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Severidad invalida: " + valor
                    + " (use baja, media, alta o critica)");
        }
    }
}
