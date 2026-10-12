package bo.phishshield.backend.empresa.infrastructure.config;

import bo.phishshield.backend.empresa.application.CrearEmpresaUseCase;
import bo.phishshield.backend.empresa.domain.port.out.EmpresaRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cablea los casos de uso de empresa sin anotar la capa application.
 */
@Configuration
public class EmpresaConfig {

    @Bean
    public CrearEmpresaUseCase crearEmpresaUseCase(EmpresaRepositoryPort empresaRepository) {
        return new CrearEmpresaUseCase(empresaRepository);
    }
}
