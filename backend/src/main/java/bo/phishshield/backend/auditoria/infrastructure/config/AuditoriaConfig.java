package bo.phishshield.backend.auditoria.infrastructure.config;

import bo.phishshield.backend.auditoria.application.ListarAuditoriaUseCase;
import bo.phishshield.backend.auditoria.domain.port.out.AuditoriaQueryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cablea el caso de uso del registro de accesos sin anotar la capa application.
 */
@Configuration
public class AuditoriaConfig {

    @Bean
    public ListarAuditoriaUseCase listarAuditoriaUseCase(AuditoriaQueryPort consultas) {
        return new ListarAuditoriaUseCase(consultas);
    }
}
