package bo.phishshield.backend.qa.domain.port.out;

import bo.phishshield.backend.qa.domain.model.AnalisisFallo;
import bo.phishshield.backend.qa.domain.model.ContextoFallo;
import bo.phishshield.backend.qa.domain.model.Ejecucion;

import java.util.Optional;

/**
 * Asistente de IA (LLM) del Centro de Pruebas. Es opcional: sin proveedor
 * configurado, estaDisponible() es false y todo funciona sin el.
 *
 * Nunca decide el estado de un caso: PASS o FAIL lo decide la prueba.
 * En la fase 7 se agregan la conversion de requisitos a Gherkin y el
 * generador de URL de phishing boliviano.
 */
public interface AsistenteIAPort {

    boolean estaDisponible();

    /** Causa probable de un FAIL y la evidencia que la respalda; vacio si la IA no responde. */
    Optional<AnalisisFallo> analizarFallo(ContextoFallo contexto);

    /** Resumen ejecutivo de una ejecucion terminada; vacio si la IA no responde. */
    Optional<String> resumirEjecucion(Ejecucion ejecucion);
}
