package bo.phishshield.backend.qa.infrastructure.config;

import bo.phishshield.backend.qa.application.MarcarInterrumpidasAlArrancarUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Al terminar de arrancar, marca como FALLIDA cualquier ejecucion QA que
 * haya quedado activa por un reinicio. Si la base no responde, solo avisa:
 * el backend sigue arrancando.
 */
@Component
public class MarcarInterrumpidasAlArrancar {

    private static final Logger log = LoggerFactory.getLogger(MarcarInterrumpidasAlArrancar.class);

    private final MarcarInterrumpidasAlArrancarUseCase marcarInterrumpidas;

    public MarcarInterrumpidasAlArrancar(MarcarInterrumpidasAlArrancarUseCase marcarInterrumpidas) {
        this.marcarInterrumpidas = marcarInterrumpidas;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void alArrancar() {
        try {
            int marcadas = marcarInterrumpidas.ejecutar();
            if (marcadas > 0) {
                log.warn("Centro de Pruebas: {} ejecucion(es) quedaron interrumpidas por un reinicio y se marcaron FALLIDA",
                        marcadas);
            }
        } catch (RuntimeException ex) {
            log.warn("Centro de Pruebas: no se pudieron revisar las ejecuciones interrumpidas: {}", ex.getMessage());
        }
    }
}
