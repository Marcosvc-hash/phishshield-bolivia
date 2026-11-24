package bo.phishshield.backend.qa.infrastructure.config;

import bo.phishshield.backend.qa.application.AsistenteQaUseCase;
import bo.phishshield.backend.qa.application.CorrerEjecucionUseCase;
import bo.phishshield.backend.qa.domain.port.out.AsistenteIAPort;
import bo.phishshield.backend.qa.domain.port.out.NotificadorCorreoPort;
import bo.phishshield.backend.qa.infrastructure.correo.NotificadorCorreoSmtpAdapter;
import bo.phishshield.backend.qa.infrastructure.ia.AsistenteClaudeAdapter;
import bo.phishshield.backend.qa.application.DetenerEjecucionUseCase;
import bo.phishshield.backend.qa.application.IniciarEjecucionUseCase;
import bo.phishshield.backend.qa.application.ListarCatalogoUseCase;
import bo.phishshield.backend.qa.application.ListarEjecucionesUseCase;
import bo.phishshield.backend.qa.application.MarcarInterrumpidasAlArrancarUseCase;
import bo.phishshield.backend.qa.application.ObtenerDetalleEjecucionUseCase;
import bo.phishshield.backend.qa.application.ObtenerEvidenciaUseCase;
import bo.phishshield.backend.qa.application.ObtenerReporteUseCase;
import bo.phishshield.backend.qa.application.PublicarResultadosUseCase;
import bo.phishshield.backend.qa.application.TrasEjecucion;
import bo.phishshield.backend.qa.domain.port.out.CatalogoCasosPort;
import bo.phishshield.backend.qa.domain.port.out.EjecucionRepositoryPort;
import bo.phishshield.backend.qa.domain.port.out.EjecutorPruebasPort;
import bo.phishshield.backend.qa.domain.port.out.EvidenciasPort;
import bo.phishshield.backend.qa.domain.port.out.GeneradorReportePort;
import bo.phishshield.backend.qa.domain.port.out.LoteRobustezPort;
import bo.phishshield.backend.qa.domain.port.out.PlanificadorEjecucionPort;
import bo.phishshield.backend.qa.infrastructure.ejecutor.JUnitEjecutorPruebasAdapter;
import bo.phishshield.backend.qa.infrastructure.ejecutor.RegistroCasosQA;
import bo.phishshield.backend.qa.infrastructure.reporte.CarpetaEvidencias;
import bo.phishshield.backend.qa.infrastructure.reporte.EvidenciasArchivoAdapter;
import bo.phishshield.backend.qa.infrastructure.reporte.GeneradorReporteAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Cablea la vertical qa sin anotar la capa application.
 *
 * @EnableAsync activa @Async (lo usa PlanificadorEjecucionAsync). Es como
 * encolar trabajo en un BackgroundService/IHostedService de .NET: la
 * peticion HTTP vuelve enseguida y las pruebas corren en otro hilo.
 */
@Configuration
@EnableAsync
@EnableConfigurationProperties({QaPropiedades.class, CorreoPropiedades.class, AsistentePropiedades.class})
public class QaConfig {

    /** Nombre del pool propio de las ejecuciones (se usa en @Async("...")). */
    public static final String POOL_EJECUCIONES = "qaPoolEjecuciones";

    private static final Clock RELOJ = Clock.systemDefaultZone();
    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");

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
                                               @Value("${spring.datasource.url:}") String urlBaseDatos,
                                               LoteRobustezPort lote) {
        return new JUnitEjecutorPruebasAdapter(registro, propiedades, iaUrl, urlBaseDatos, RELOJ, lote);
    }

    @Bean
    public CarpetaEvidencias carpetaEvidencias(QaPropiedades propiedades) {
        return CarpetaEvidencias.desde(propiedades);
    }

    @Bean
    public GeneradorReportePort generadorReporte(CarpetaEvidencias carpeta) {
        return new GeneradorReporteAdapter(carpeta, RELOJ);
    }

    @Bean
    public EvidenciasPort evidencias(CarpetaEvidencias carpeta) {
        return new EvidenciasArchivoAdapter(carpeta);
    }

    @Bean
    public NotificadorCorreoPort notificadorCorreo(CorreoPropiedades propiedades, CarpetaEvidencias carpeta) {
        return new NotificadorCorreoSmtpAdapter(propiedades, carpeta);
    }

    @Bean
    public AsistenteIAPort asistenteIA(AsistentePropiedades propiedades) {
        return new AsistenteClaudeAdapter(propiedades);
    }

    /** Al terminar cada ejecucion: generar el PDF y el HTML y, si hay SMTP, enviarlos. */
    @Bean
    public TrasEjecucion trasEjecucion(EjecucionRepositoryPort ejecuciones, GeneradorReportePort generador,
                                       NotificadorCorreoPort notificador) {
        return new PublicarResultadosUseCase(ejecuciones, generador, notificador, RELOJ, ZONA);
    }

    @Bean
    public AsistenteQaUseCase asistenteQaUseCase(EjecucionRepositoryPort ejecuciones, AsistenteIAPort asistente,
                                                 NotificadorCorreoPort notificador) {
        return new AsistenteQaUseCase(ejecuciones, asistente, notificador);
    }

    @Bean
    public ObtenerEvidenciaUseCase obtenerEvidenciaUseCase(EvidenciasPort evidencias) {
        return new ObtenerEvidenciaUseCase(evidencias);
    }

    @Bean
    public ObtenerReporteUseCase obtenerReporteUseCase(EjecucionRepositoryPort ejecuciones,
                                                       GeneradorReportePort generador, EvidenciasPort evidencias) {
        return new ObtenerReporteUseCase(ejecuciones, generador, evidencias);
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
