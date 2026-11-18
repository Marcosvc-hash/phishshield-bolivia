package bo.phishshield.backend.qa.domain.port.out;

import bo.phishshield.backend.qa.domain.model.PlanEjecucion;

import java.util.UUID;

/**
 * Ejecuta de verdad los casos del plan. La implementacion usa el JUnit
 * Platform Launcher, pero el dominio no lo sabe.
 */
public interface EjecutorPruebasPort {

    /**
     * Corre los casos del plan y avisa cada desenlace al oyente apenas el
     * caso termina. Bloquea hasta que terminan todos.
     *
     * Un caso que falla NO es una excepcion: se informa como FAIL. Este
     * metodo solo lanza una excepcion si el motor de pruebas mismo no pudo
     * funcionar; en ese caso los casos que no alcanzaron a correr se quedan
     * sin desenlace (NOT_RUN).
     */
    void ejecutar(PlanEjecucion plan, OyenteEjecucion oyente);

    /**
     * Pide detener la ejecucion indicada. Los casos que todavia no empezaron
     * no se ejecutan. Devuelve false si esa ejecucion no esta corriendo.
     */
    boolean detener(UUID idEjecucion);
}
