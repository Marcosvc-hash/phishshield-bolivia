package bo.phishshield.backend.qa.application;

import bo.phishshield.backend.qa.domain.model.DesenlaceCaso;
import bo.phishshield.backend.qa.domain.model.Ejecucion;
import bo.phishshield.backend.qa.domain.model.EstadoEjecucion;
import bo.phishshield.backend.qa.domain.model.FinEjecucion;
import bo.phishshield.backend.qa.domain.model.PlanEjecucion;
import bo.phishshield.backend.qa.domain.model.TransicionEjecucionInvalidaException;
import bo.phishshield.backend.qa.domain.port.out.EjecucionRepositoryPort;
import bo.phishshield.backend.qa.domain.port.out.EjecutorPruebasPort;
import bo.phishshield.backend.qa.domain.port.out.OyenteEjecucion;

import java.lang.System.Logger.Level;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Caso de uso: correr una ejecucion PENDIENTE. Lo llama el planificador en
 * su propio hilo, nunca un controlador.
 *
 *   PENDIENTE -> EN_CURSO -> (cada caso se guarda al terminar) -> FINALIZADA
 *                                                            \-> FALLIDA (detenida o error del motor)
 *
 * Al terminar corren los pasos posteriores (reportes, correo); si uno de
 * ellos falla, el resultado de las pruebas no cambia.
 */
public class CorrerEjecucionUseCase {

    static final String MOTIVO_DETENIDA = "Detenida por el usuario";

    private static final System.Logger log = System.getLogger(CorrerEjecucionUseCase.class.getName());

    private final EjecucionRepositoryPort ejecuciones;
    private final EjecutorPruebasPort ejecutor;
    private final TrasEjecucion trasEjecucion;
    private final Clock reloj;

    public CorrerEjecucionUseCase(EjecucionRepositoryPort ejecuciones, EjecutorPruebasPort ejecutor,
                                  TrasEjecucion trasEjecucion, Clock reloj) {
        this.ejecuciones = ejecuciones;
        this.ejecutor = ejecutor;
        this.trasEjecucion = trasEjecucion;
        this.reloj = reloj;
    }

    public void ejecutar(UUID idEjecucion) {
        Ejecucion ejecucion = ejecuciones.buscarPorUuid(idEjecucion).orElse(null);
        if (ejecucion == null || ejecucion.getEstado() != EstadoEjecucion.PENDIENTE) {
            // Ya la detuvieron antes de empezar, o la corrio otro hilo
            log.log(Level.INFO, "La ejecucion {0} no esta pendiente; no se corre", idEjecucion);
            return;
        }

        ejecucion.iniciar(ahora());
        try {
            ejecuciones.guardar(ejecucion);
        } catch (TransicionEjecucionInvalidaException ex) {
            log.log(Level.INFO, "La ejecucion {0} cambio de estado antes de empezar: {1}",
                    idEjecucion, ex.getMessage());
            return;
        }

        try {
            FinEjecucion fin = ejecutor.ejecutar(PlanEjecucion.de(ejecucion), new GuardarAlLlegar(ejecucion));
            if (fin == FinEjecucion.DETENIDA) {
                ejecucion.fallar(MOTIVO_DETENIDA, ahora());
            } else {
                ejecucion.finalizar(ahora());
            }
        } catch (RuntimeException ex) {
            log.log(Level.WARNING, "El motor de pruebas fallo en la ejecucion " + idEjecucion, ex);
            if (ejecucion.getEstado().estaActiva()) {
                ejecucion.fallar("El motor de pruebas se detuvo por un error: " + ex.getMessage(), ahora());
            }
        }

        try {
            ejecuciones.guardar(ejecucion);
        } catch (RuntimeException ex) {
            // Por ejemplo, la detuvieron "a mano" mientras corria: lo que quedo en la base manda
            log.log(Level.WARNING, "No se pudo guardar el final de la ejecucion " + idEjecucion, ex);
            return;
        }

        trasEjecucion.alTerminar(ejecucion);
    }

    private OffsetDateTime ahora() {
        return OffsetDateTime.now(reloj);
    }

    /**
     * Guarda cada desenlace apenas llega: la pantalla ve el avance y, si el
     * backend se cae, lo ya ejecutado no se pierde. Un error al guardar no
     * corta las pruebas: el estado queda en memoria y se guarda al final.
     */
    private final class GuardarAlLlegar implements OyenteEjecucion {

        private final Ejecucion ejecucion;

        private GuardarAlLlegar(Ejecucion ejecucion) {
            this.ejecucion = ejecucion;
        }

        @Override
        public synchronized void casoTerminado(DesenlaceCaso desenlace) {
            ejecucion.registrarResultado(desenlace);
            guardar();
        }

        @Override
        public synchronized void casoNoEjecutado(String codigo, String motivo) {
            ejecucion.anotarNoEjecutado(codigo, motivo);
            guardar();
        }

        private void guardar() {
            try {
                ejecuciones.guardar(ejecucion);
            } catch (RuntimeException ex) {
                log.log(Level.WARNING, "No se pudo guardar el avance de la ejecucion "
                        + ejecucion.getUuidPublico() + ": " + ex.getMessage());
            }
        }
    }
}
