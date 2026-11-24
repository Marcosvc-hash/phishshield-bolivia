package bo.phishshield.backend.qa.infrastructure.ejecutor;

import bo.phishshield.backend.qa.application.CorrerEjecucionUseCase;
import bo.phishshield.backend.qa.domain.port.out.PlanificadorEjecucionPort;
import bo.phishshield.backend.qa.infrastructure.config.QaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Pone la ejecucion a correr en el pool propio de un hilo y vuelve enseguida.
 *
 * @Async hace que Spring llame a este metodo en otro hilo (como un
 * Task.Run(...) de C# sobre una cola de un solo trabajador). Solo funciona
 * si se llama desde OTRO bean: Spring envuelve la clase en un proxy.
 */
@Component
public class PlanificadorEjecucionAsync implements PlanificadorEjecucionPort {

    private static final Logger log = LoggerFactory.getLogger(PlanificadorEjecucionAsync.class);

    private final CorrerEjecucionUseCase correrEjecucion;

    public PlanificadorEjecucionAsync(CorrerEjecucionUseCase correrEjecucion) {
        this.correrEjecucion = correrEjecucion;
    }

    @Async(QaConfig.POOL_EJECUCIONES)
    @Override
    public void programar(UUID idEjecucion) {
        try {
            correrEjecucion.ejecutar(idEjecucion);
        } catch (RuntimeException ex) {
            log.error("La ejecucion QA {} termino con un error inesperado", idEjecucion, ex);
        }
    }
}
