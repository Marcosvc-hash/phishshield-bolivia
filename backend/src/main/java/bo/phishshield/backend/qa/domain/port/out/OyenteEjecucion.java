package bo.phishshield.backend.qa.domain.port.out;

import bo.phishshield.backend.qa.domain.model.DesenlaceCaso;

/**
 * Quien recibe los desenlaces mientras la ejecucion avanza. Lo implementa
 * el caso de uso, que guarda cada resultado apenas llega: asi la pantalla
 * muestra el avance y, si el backend se cae, lo ya ejecutado no se pierde.
 */
@FunctionalInterface
public interface OyenteEjecucion {

    void casoTerminado(DesenlaceCaso desenlace);
}
