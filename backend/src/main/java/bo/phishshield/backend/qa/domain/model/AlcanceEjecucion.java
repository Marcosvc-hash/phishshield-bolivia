package bo.phishshield.backend.qa.domain.model;

/** Si se ejecuto la suite entera o solo los casos marcados (chk_qa_ejec_alcance). */
public enum AlcanceEjecucion {

    SUITE_COMPLETA("Suite completa"),
    SELECCION("Casos seleccionados");

    private final String etiqueta;

    AlcanceEjecucion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public String aValorBd() {
        return ValorBd.de(this);
    }

    public static AlcanceEjecucion desdeValorBd(String valor) {
        return ValorBd.convertir(AlcanceEjecucion.class, valor, "el alcance de la ejecución");
    }
}
