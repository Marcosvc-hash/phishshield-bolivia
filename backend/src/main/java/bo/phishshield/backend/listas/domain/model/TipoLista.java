package bo.phishshield.backend.listas.domain.model;

/**
 * Las listas de dominios_lista (chk_dominios_lista).
 *   blanca: legitimos verificados; el motor no los acusa de typosquatting
 *           ni le pregunta a la IA.
 *   negra:  reportados como maliciosos; una coincidencia basta para phishing.
 *   gris:   en observacion; existe en los datos pero el motor no la usa,
 *           asi que desde el panel no se crean filas nuevas.
 */
public enum TipoLista {

    BLANCA,
    NEGRA,
    GRIS;

    public boolean permiteAltas() {
        return this != GRIS;
    }

    /** La lista con la que no se puede convivir: blanca <-> negra. Gris no tiene. */
    public TipoLista opuesta() {
        return switch (this) {
            case BLANCA -> NEGRA;
            case NEGRA -> BLANCA;
            case GRIS -> null;
        };
    }

    public String aValorBd() {
        return name().toLowerCase();
    }

    public static TipoLista desdeValorBd(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Indique la lista (blanca o negra)");
        }
        try {
            return valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Lista invalida: " + valor + " (use blanca o negra)");
        }
    }
}
