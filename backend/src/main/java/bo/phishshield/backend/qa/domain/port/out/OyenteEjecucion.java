package bo.phishshield.backend.qa.domain.port.out;

import bo.phishshield.backend.qa.domain.model.DesenlaceCaso;

/**
 * Quien recibe los desenlaces mientras la ejecucion avanza. Lo implementa
 * el caso de uso, que guarda cada resultado apenas llega: asi la pantalla
 * muestra el avance y, si el backend se cae, lo ya ejecutado no se pierde.
 */
public interface OyenteEjecucion {

    /** El caso corrio y termino en PASS, FAIL o SKIPPED. */
    void casoTerminado(DesenlaceCaso desenlace);

    /**
     * El caso no se pudo ejecutar y queda en NOT_RUN, por ejemplo porque falta
     * una variable de entorno, porque se implementa en una fase posterior o
     * porque la ejecucion se detuvo antes de llegar a el.
     */
    void casoNoEjecutado(String codigo, String motivo);
}
