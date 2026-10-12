package bo.phishshield.backend.analisis.infrastructure.config;

import bo.phishshield.backend.analisis.application.AnalizarUrlUseCase;
import bo.phishshield.backend.analisis.domain.port.out.AnalisisRepositoryPort;
import bo.phishshield.backend.analisis.domain.port.out.DominioOficialRepositoryPort;
import bo.phishshield.backend.analisis.domain.port.out.ListaBlancaRepositoryPort;
import bo.phishshield.backend.analisis.domain.port.out.ListaNegraRepositoryPort;
import bo.phishshield.backend.analisis.domain.port.out.ModeloIaPort;
import bo.phishshield.backend.analisis.domain.port.out.ModeloIaRepositoryPort;
import bo.phishshield.backend.analisis.domain.port.out.ReglaDeteccionRepositoryPort;
import bo.phishshield.backend.analisis.domain.service.ReglaIpLiteral;
import bo.phishshield.backend.analisis.domain.service.ReglaRutaSensible;
import bo.phishshield.backend.analisis.domain.service.ReglaSinHttps;
import bo.phishshield.backend.analisis.domain.service.ReglaSubdominios;
import bo.phishshield.backend.analisis.domain.service.ReglaTldSospechoso;
import bo.phishshield.backend.analisis.domain.service.ReglaUrl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.List;

/**
 * Cablea el dominio con Spring sin que el dominio se entere.
 * Las clases de domain/ y application/ no llevan ninguna anotacion:
 * se instancian aca.
 */
@Configuration
public class AnalisisConfig {

    /**
     * Reglas sin estado. ReglaTyposquatting y ReglaListaNegra no estan aca
     * porque necesitan datos de la base que se leen en cada ejecucion.
     */
    @Bean
    public List<ReglaUrl> reglasUrlBase() {
        return List.of(
                new ReglaIpLiteral(),
                new ReglaSinHttps(),
                new ReglaTldSospechoso(),
                new ReglaSubdominios(),
                new ReglaRutaSensible()
        );
    }

    @Bean
    public AnalizarUrlUseCase analizarUrlUseCase(
            List<ReglaUrl> reglasUrlBase,
            AnalisisRepositoryPort analisisRepository,
            ReglaDeteccionRepositoryPort reglaRepository,
            DominioOficialRepositoryPort dominioRepository,
            ListaNegraRepositoryPort listaNegraRepository,
            ListaBlancaRepositoryPort listaBlancaRepository,
            ModeloIaPort modeloIa,
            ModeloIaRepositoryPort modeloIaRepository) {

        return new AnalizarUrlUseCase(
                reglasUrlBase,
                analisisRepository,
                reglaRepository,
                dominioRepository,
                listaNegraRepository,
                listaBlancaRepository,
                modeloIa,
                modeloIaRepository,
                Clock.systemDefaultZone());
    }
}