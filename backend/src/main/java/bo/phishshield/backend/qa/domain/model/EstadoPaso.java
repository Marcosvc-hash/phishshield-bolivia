package bo.phishshield.backend.qa.domain.model;

/** Si un paso se cumplio o fue donde fallo el caso (chk_qa_pasos_estado). */
public enum EstadoPaso {

    OK,
    FALLO;

    public String aValorBd() {
        return ValorBd.de(this);
    }

    public static EstadoPaso desdeValorBd(String valor) {
        return ValorBd.convertir(EstadoPaso.class, valor, "el estado del paso");
    }
}
