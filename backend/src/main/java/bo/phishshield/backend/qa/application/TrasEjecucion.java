package bo.phishshield.backend.qa.application;

import bo.phishshield.backend.qa.domain.model.Ejecucion;

/**
 * Lo que se hace cuando una ejecucion termina (generar los reportes,
 * enviarlos por correo). Nunca lanza excepciones: un fallo aqui no cambia
 * el resultado de las pruebas.
 */
@FunctionalInterface
public interface TrasEjecucion {

    TrasEjecucion NADA = ejecucion -> { };

    void alTerminar(Ejecucion ejecucion);
}
