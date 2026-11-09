package bo.phishshield.backend.panel.infrastructure.config;

import bo.phishshield.backend.panel.application.ObtenerPanelUseCase;
import bo.phishshield.backend.panel.domain.port.out.PanelQueryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Cablea el caso de uso del panel sin anotar la capa application.
 */
@Configuration
public class PanelConfig {

    @Bean
    public ObtenerPanelUseCase obtenerPanelUseCase(PanelQueryPort consultas) {
        return new ObtenerPanelUseCase(consultas, Clock.systemDefaultZone());
    }
}
