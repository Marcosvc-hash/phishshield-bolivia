package bo.phishshield.backend.qa.domain.model;

/** Con que se ejecuta un caso (chk_qa_casos_herramienta). */
public enum Herramienta {

    API("API (Rest Assured)"),
    UI("Interfaz (Playwright)"),
    CARGA("Generador de carga"),
    MODELO("Lote etiquetado");

    private final String etiqueta;

    Herramienta(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public String aValorBd() {
        return ValorBd.de(this);
    }

    public static Herramienta desdeValorBd(String valor) {
        return ValorBd.convertir(Herramienta.class, valor, "la herramienta");
    }
}
