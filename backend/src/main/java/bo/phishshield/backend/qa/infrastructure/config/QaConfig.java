package bo.phishshield.backend.qa.infrastructure.config;

import bo.phishshield.backend.qa.application.CorrerEjecucionUseCase;
import bo.phishshield.backend.qa.application.DetenerEjecucionUseCase;
import bo.phishshield.backend.qa.application.IniciarEjecucionUseCase;
import bo.phishshield.backend.qa.application.ListarCatalogoUseCase;
import bo.phishshield.backend.qa.application.ListarEjecucionesUseCase;
import bo.phishshield.backend.qa.application.MarcarInterrumpidasAlArrancarUseCase;
import bo.phishshield.backend.qa.application.ObtenerDetalleEjecucionUseCase;
import bo.phishshield.backend.qa.application.TrasEjecucion;
import bo.phishshield.backend.qa.domain.port.out.CatalogoCasosPort;
import bo.phishshield.backend.qa.domain.port.out.EjecucionRepositoryPort;
import bo.phishshield.backend.qa.domain.port.out.EjecutorPruebasPort;
import bo.phishshield.backend.qa.domain.port.out.PlanificadorEjecucionPort;
import bo.phishshield.backend.qa.infrastructure.ejecutor.JUnitEjecutorPruebasAdapter;
import bo.phishshield.backend.qa.infrastructure.ejecutor.RegistroCasosQA;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Clock;

/**
 * Cablea la vertical qa sin anotar la capa application.
 *
 * @EnableAsync activa @Async (lo usa PlanificadorEjecucionAsync). Es como
 * encolar trabajo en un BackgroundService/IHostedService de .NET: la
 * peticion HTTP vuelve enseguida y las pruebas corren en otro hilo.
 */
@Configuration
@EnableAsync
@EnableConfigurationProperties(QaPropiedades.class)
public class QaConfig {

    /** Nombre del pool propio de las ejecuciones (se usa en @Async("...")). */
    public static final String POOL_EJECUCIONES = "qaPoolEjecuciones";

    private static final Clock RELOJ = Clock.systemDefaultZone();

    /**
     * Un solo hilo: las ejecuciones nunca corren en paralelo (una prueba de
     * carga no debe mezclarse con otra). La cola es corta porque la base ya
     * impide tener mas de una activa.
     */
    @Bean(name = POOL_EJECUCIONES)
    public ThreadPoolTaskExecutor qaPoolEjecuciones() {
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setCorePoolSize(1);
        pool.setMaxPoolSize(1);
        pool.setQueueCapacity(5);
        pool.setThreadNamePrefix("qa-ejecucion-");
        pool.setWaitForTasksToCompleteOnShutdown(false);
        pool.initialize();
        return pool;
    }

    @Bean
    public RegistroCasosQA registroCasosQA() {
        return new RegistroCasosQA();
    }

    @Bean
    public EjecutorPruebasPort ejecutorPruebas(RegistroCasosQA registro, QaPropiedades propiedades,
                                               @Value("${phishshield.ia.url:}") String iaUrl,
                                               @Value("${spring.datasource.url:}") String urlBaseDatos) {
        return new JUnitEjecutorPruebasAdapter(registro, propiedades, iaUrl, urlBaseDatos, RELOJ);
    }

    /** En la fase 3 se reemplaza por la generacion de reportes (y en la 7, el correo). */
    @Bean
    public TrasEjecucion trasEjecucion() {
        return TrasEjecucion.NADA;
    }

    @Bean
    public IniciarEjecucionUseCase iniciarEjecucionUseCase(CatalogoCasosPort catalogo,
                                                           EjecucionRepositoryPort ejecuciones,
                                                           PlanificadorEjecucionPort planificador,
                                                           QaPropiedades propiedades) {
        String ambiente = propiedades.ambiente() == null || propiedades.ambiente().isBlank()
                ? "local" : propiedades.ambiente();
        return new IniciarEjecucionUseCase(catalogo, ejecuciones, planificador, ambiente, RELOJ);
    }

    @Bean
    public CorrerEjecucionUseCase correrEjecucionUseCase(EjecucionRepositoryPort ejecuciones,
                                                         EjecutorPruebasPort ejecutor,
                                                         TrasEjecucion trasEjecucion) {
        return new CorrerEjecucionUseCase(ejecuciones, ejecutor, trasEjecucion, RELOJ);
    }

    @Bean
    public DetenerEjecucionUseCase detenerEjecucionUseCase(EjecucionRepositoryPort ejecuciones,
                                                           EjecutorPruebasPort ejecutor) {
        return new DetenerEjecucionUseCase(ejecuciones, ejecutor, RELOJ);
    }

    @Bean
    public ListarEjecucionesUseCase listarEjecucionesUseCase(EjecucionRepositoryPort ejecuciones) {
        return new ListarEjecucionesUseCase(ejecuciones);
    }

    @Bean
    public ObtenerDetalleEjecucionUseCase obtenerDetalleEjecucionUseCase(EjecucionRepositoryPort ejecuciones) {
        return new ObtenerDetalleEjecucionUseCase(ejecuciones);
    }

    @Bean
    public ListarCatalogoUseCase listarCatalogoUseCase(CatalogoCasosPort catalogo, EjecutorPruebasPort ejecutor) {
        return new ListarCatalogoUseCase(catalogo, ejecutor);
    }

    @Bean
    public MarcarInterrumpidasAlArrancarUseCase marcarInterrumpidasAlArrancarUseCase(
            EjecucionRepositoryPort ejecuciones) {
        return new MarcarInterrumpidasAlArrancarUseCase(ejecuciones, RELOJ);
    }
}
