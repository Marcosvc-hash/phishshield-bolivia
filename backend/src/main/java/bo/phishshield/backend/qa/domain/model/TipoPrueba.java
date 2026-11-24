package bo.phishshield.backend.qa.domain.model;

/**
 * Gran division de las pruebas (chk_qa_casos_tipo). Los casos se tipifican
 * solo por tipo y suite de prueba, nunca como "positivos" o "negativos".
 */
public enum TipoPrueba {

    FUNCIONAL("Funcional"),
    NO_FUNCIONAL("No funcional");

    private final String etiqueta;

    TipoPrueba(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Texto para la pantalla y los reportes. */
    public String etiqueta() {
        return etiqueta;
    }

    public String aValorBd() {
        return ValorBd.de(this);
    }

    public static TipoPrueba desdeValorBd(String valor) {
        return ValorBd.convertir(TipoPrueba.class, valor, "el tipo de prueba");
    }
}
