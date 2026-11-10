package bo.phishshield.backend.listas.infrastructure.config;

import bo.phishshield.backend.listas.application.AgregarDominioUseCase;
import bo.phishshield.backend.listas.application.CambiarEstadoDominioUseCase;
import bo.phishshield.backend.listas.application.EditarDominioUseCase;
import bo.phishshield.backend.listas.application.ListarDominiosUseCase;
import bo.phishshield.backend.listas.domain.port.out.DominioListaRepositoryPort;
import bo.phishshield.backend.listas.domain.port.out.EntidadSuplantadaPort;
import bo.phishshield.backend.shared.domain.port.out.AuditoriaPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cablea los casos de uso de listas de dominios sin anotar la capa application.
 */
@Configuration
public class ListasConfig {

    @Bean
    public ListarDominiosUseCase listarDominiosUseCase(DominioListaRepositoryPort dominios,
                                                       EntidadSuplantadaPort entidades) {
        return new ListarDominiosUseCase(dominios, entidades);
    }

    @Bean
    public AgregarDominioUseCase agregarDominioUseCase(DominioListaRepositoryPort dominios,
                                                       EntidadSuplantadaPort entidades,
                                                       AuditoriaPort auditoria) {
        return new AgregarDominioUseCase(dominios, entidades, auditoria);
    }

    @Bean
    public EditarDominioUseCase editarDominioUseCase(DominioListaRepositoryPort dominios,
                                                     EntidadSuplantadaPort entidades,
                                                     AuditoriaPort auditoria) {
        return new EditarDominioUseCase(dominios, entidades, auditoria);
    }

    @Bean
    public CambiarEstadoDominioUseCase cambiarEstadoDominioUseCase(DominioListaRepositoryPort dominios,
                                                                   EntidadSuplantadaPort entidades,
                                                                   AuditoriaPort auditoria) {
        return new CambiarEstadoDominioUseCase(dominios, entidades, auditoria);
    }
}
