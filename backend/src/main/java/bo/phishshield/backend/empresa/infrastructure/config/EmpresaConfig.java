package bo.phishshield.backend.empresa.infrastructure.config;

import bo.phishshield.backend.empresa.application.CambiarEstadoEmpresaUseCase;
import bo.phishshield.backend.empresa.application.CrearEmpresaUseCase;
import bo.phishshield.backend.empresa.application.EditarEmpresaUseCase;
import bo.phishshield.backend.empresa.domain.port.out.EmpresaRepositoryPort;
import bo.phishshield.backend.shared.domain.port.out.AuditoriaPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cablea los casos de uso de empresa sin anotar la capa application.
 */
@Configuration
public class EmpresaConfig {

    @Bean
    public CrearEmpresaUseCase crearEmpresaUseCase(EmpresaRepositoryPort empresaRepository,
                                                   AuditoriaPort auditoria) {
        return new CrearEmpresaUseCase(empresaRepository, auditoria);
    }

    @Bean
    public EditarEmpresaUseCase editarEmpresaUseCase(EmpresaRepositoryPort empresaRepository,
                                                     AuditoriaPort auditoria) {
        return new EditarEmpresaUseCase(empresaRepository, auditoria);
    }

    @Bean
    public CambiarEstadoEmpresaUseCase cambiarEstadoEmpresaUseCase(EmpresaRepositoryPort empresaRepository,
                                                                   AuditoriaPort auditoria) {
        return new CambiarEstadoEmpresaUseCase(empresaRepository, auditoria);
    }
}
