package bo.phishshield.backend.qa.domain.model;

/**
 * Estado de un caso dentro de una ejecucion (chk_qa_res_estado).
 *
 * NOT_RUN es el estado inicial: mientras la ejecucion esta EN_CURSO quiere
 * decir "todavia no corre"; cuando la ejecucion termino, "no se ejecuto".
 */
public enum EstadoCaso {

    PASS("Aprobado"),
    FAIL("Fallido"),
    SKIPPED("Omitido"),
    NOT_RUN("No ejecutado");

    private final String etiqueta;

    EstadoCaso(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public String aValorBd() {
        return ValorBd.de(this);
    }

    public static EstadoCaso desdeValorBd(String valor) {
        return ValorBd.convertir(EstadoCaso.class, valor, "el estado del caso");
    }
}
