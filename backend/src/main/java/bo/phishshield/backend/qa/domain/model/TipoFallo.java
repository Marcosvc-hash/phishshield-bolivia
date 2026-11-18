package bo.phishshield.backend.qa.domain.model;

/**
 * Por que fallo un caso (chk_qa_res_tipo_fallo):
 *
 * FUNCIONAL: el sistema respondio, pero no lo que se esperaba (una asercion
 *   no se cumplio). Es un defecto de PhishShield.
 * INFRAESTRUCTURA: no se pudo comprobar nada porque algo no respondio
 *   (conexion rechazada, timeout, servicio de IA caido). No dice si
 *   PhishShield funciona bien o mal.
 */
public enum TipoFallo {

    FUNCIONAL("Funcional"),
    INFRAESTRUCTURA("Infraestructura");

    private final String etiqueta;

    TipoFallo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public String aValorBd() {
        return ValorBd.de(this);
    }

    public static TipoFallo desdeValorBd(String valor) {
        return ValorBd.convertir(TipoFallo.class, valor, "el tipo de fallo");
    }
}
