package bo.phishshield.backend.alerta.infrastructure.config;

import bo.phishshield.backend.alerta.application.ActualizarAlertaUseCase;
import bo.phishshield.backend.alerta.application.ListarAlertasUseCase;
import bo.phishshield.backend.alerta.domain.port.out.AlertaRepositoryPort;
import bo.phishshield.backend.shared.domain.port.out.AuditoriaPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Cablea los casos de uso de alertas sin anotar la capa application.
 */
@Configuration
public class AlertaConfig {

    @Bean
    public ListarAlertasUseCase listarAlertasUseCase(AlertaRepositoryPort alertas) {
        return new ListarAlertasUseCase(alertas);
    }

    @Bean
    public ActualizarAlertaUseCase actualizarAlertaUseCase(AlertaRepositoryPort alertas, AuditoriaPort auditoria) {
        return new ActualizarAlertaUseCase(alertas, auditoria, Clock.systemDefaultZone());
    }
}
