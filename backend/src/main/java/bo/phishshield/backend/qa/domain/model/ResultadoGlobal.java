package bo.phishshield.backend.qa.domain.model;

import java.util.Collection;

/**
 * Resultado de una ejecucion FINALIZADA (chk_qa_ejec_resultado):
 *
 *   APROBADA   -> todos los casos en PASS
 *   CON_FALLOS -> al menos un caso en FAIL
 *   INCOMPLETA -> ningun FAIL, pero algun caso SKIPPED o NOT_RUN: no se
 *                 puede afirmar que todo funciona si algo no se comprobo
 */
public enum ResultadoGlobal {

    APROBADA("Aprobada"),
    CON_FALLOS("Con fallos"),
    INCOMPLETA("Incompleta");

    private final String etiqueta;

    ResultadoGlobal(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public static ResultadoGlobal calcular(Collection<EstadoCaso> estados) {
        if (estados.contains(EstadoCaso.FAIL)) {
            return CON_FALLOS;
        }
        if (!estados.isEmpty() && estados.stream().allMatch(e -> e == EstadoCaso.PASS)) {
            return APROBADA;
        }
        return INCOMPLETA;
    }

    public String aValorBd() {
        return ValorBd.de(this);
    }

    public static ResultadoGlobal desdeValorBd(String valor) {
        return ValorBd.convertir(ResultadoGlobal.class, valor, "el resultado global");
    }
}
