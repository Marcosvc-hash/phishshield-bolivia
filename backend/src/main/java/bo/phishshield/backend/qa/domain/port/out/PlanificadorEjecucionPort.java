package bo.phishshield.backend.qa.domain.port.out;

import java.util.UUID;

/**
 * Pone una ejecucion PENDIENTE a correr en segundo plano y vuelve enseguida.
 *
 * Existe para que la capa application no lleve anotaciones de Spring: la
 * implementacion (en infrastructure) es la que usa @Async con su propio
 * pool de hilos.
 */
public interface PlanificadorEjecucionPort {

    void programar(UUID idEjecucion);
}
