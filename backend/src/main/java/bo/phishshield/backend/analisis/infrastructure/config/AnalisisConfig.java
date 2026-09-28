package bo.phishshield.backend.analisis.infrastructure.config;

import bo.phishshield.backend.analisis.application.AnalizarUrlUseCase;
import bo.phishshield.backend.analisis.domain.port.out.AnalisisRepositoryPort;
import bo.phishshield.backend.analisis.domain.port.out.DominioOficialRepositoryPort;
import bo.phishshield.backend.analisis.domain.port.out.ReglaDeteccionRepositoryPort;
import bo.phishshield.backend.analisis.domain.service.ReglaIpLiteral;
import bo.phishshield.backend.analisis.domain.service.ReglaSinHttps;
import bo.phishshield.backend.analisis.domain.service.ReglaSubdominios;
import bo.phishshield.backend.analisis.domain.service.ReglaTldSospechoso;
import bo.phishshield.backend.analisis.domain.service.ReglaUrl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Cablea el dominio con Spring sin que el dominio se entere.
 * Las clases de domain/ y application/ no llevan ninguna anotacion:
 * se instancian aca.
 */
@Configuration
public class AnalisisConfig {

    /**
     * Reglas sin estado. ReglaTyposquatting no esta aca porque necesita
     * la lista de dominios oficiales, que se lee en cada ejecucion.
     */
    @Bean
    public List<ReglaUrl> reglasUrlBase() {
        return List.of(
                new ReglaIpLiteral(),
                new ReglaSinHttps(),
                new ReglaTldSospechoso(),
                new ReglaSubdominios()
        );
    }

    @Bean
    public AnalizarUrlUseCase analizarUrlUseCase(
            List<ReglaUrl> reglasUrlBase,
            AnalisisRepositoryPort analisisRepository,
            ReglaDeteccionRepositoryPort reglaRepository,
            DominioOficialRepositoryPort dominioRepository) {

        return new AnalizarUrlUseCase(
                reglasUrlBase,
                analisisRepository,
                reglaRepository,
                dominioRepository);
    }
}