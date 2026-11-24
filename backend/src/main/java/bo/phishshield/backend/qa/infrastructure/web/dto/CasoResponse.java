package bo.phishshield.backend.qa.infrastructure.web.dto;

import bo.phishshield.backend.qa.domain.model.CasoCatalogo;
import bo.phishshield.backend.qa.domain.model.CasoPrueba;

/** Un caso del catalogo. El "id" es el codigo (TC-SMK-001): el BIGINT interno no sale de la API. */
public record CasoResponse(
        String codigo,
        String tipo,
        String tipoEtiqueta,
        String suite,
        String suiteEtiqueta,
        String herramienta,
        String herramientaEtiqueta,
        int orden,
        String nombre,
        String objetivo,
        String precondiciones,
        String datosPrueba,
        String resultadoEsperado,
        boolean implementado
) {

    public static CasoResponse desde(CasoCatalogo item) {
        CasoPrueba c = item.caso();
        return new CasoResponse(
                c.codigo(),
                c.tipo().aValorBd(), c.tipo().etiqueta(),
                c.suite().aValorBd(), c.suite().etiqueta(),
                c.herramienta().aValorBd(), c.herramienta().etiqueta(),
                c.orden(), c.nombre(), c.objetivo(), c.precondiciones(), c.datosPrueba(), c.resultadoEsperado(),
                item.implementado());
    }
}
