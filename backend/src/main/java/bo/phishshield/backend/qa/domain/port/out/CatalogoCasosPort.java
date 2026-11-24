package bo.phishshield.backend.qa.domain.port.out;

import bo.phishshield.backend.qa.domain.model.CasoPrueba;
import bo.phishshield.backend.qa.domain.model.Suite;

import java.util.Collection;
import java.util.List;

/** Lee el catalogo de casos (tabla qa_casos). */
public interface CatalogoCasosPort {

    /** Todos los casos activos, ordenados por suite y orden. */
    List<CasoPrueba> listarActivos();

    /** Los casos activos de una suite, en su orden. */
    List<CasoPrueba> listarPorSuite(Suite suite);

    /** Los casos con esos codigos (activos o no); los codigos que no existen simplemente no vuelven. */
    List<CasoPrueba> buscarPorCodigos(Collection<String> codigos);
}
