package bo.phishshield.backend.qa.infrastructure.ejecutor;

import bo.phishshield.backend.qa.domain.model.DesenlaceCaso;
import bo.phishshield.backend.qa.domain.model.FinEjecucion;
import bo.phishshield.backend.qa.domain.model.PlanEjecucion;
import bo.phishshield.backend.qa.domain.model.TipoFallo;
import bo.phishshield.backend.qa.domain.port.out.EjecutorPruebasPort;
import bo.phishshield.backend.qa.domain.port.out.OyenteEjecucion;
import bo.phishshield.backend.qa.infrastructure.config.QaPropiedades;
import bo.phishshield.backend.qa.infrastructure.reporte.CarpetaEvidencias;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

/**
 * Ejecuta los casos con el JUnit Platform Launcher, la misma API que usan
 * Maven e IntelliJ para correr pruebas, pero en caliente, dentro del backend.
 *
 * Cada caso se lanza por separado y en el orden del plan: asi el avance se
 * guarda caso por caso y "detener" actua entre un caso y el siguiente.
 *
 * Como se traduce lo que informa JUnit:
 *   exito                                   -> PASS
 *   AssertionError (una asercion fallo)     -> FAIL funcional
 *   sin conexion, timeout, IA caida...      -> FAIL de infraestructura
 *   CasoNoEjecutableException (falta config) -> NOT_RUN con el motivo
 *   otra suposicion no cumplida (assume)    -> SKIPPED
 */
public class JUnitEjecutorPruebasAdapter implements EjecutorPruebasPort {

    static final String NO_IMPLEMENTADO = "Se implementa en una fase posterior";
    static final String DETENIDA = "La ejecución se detuvo antes de llegar a este caso";

    private static final Logger log = LoggerFactory.getLogger(JUnitEjecutorPruebasAdapter.class);

    private final RegistroCasosQA registro;
    private final QaPropiedades propiedades;
    private final String iaUrl;
    private final String urlBaseDatos;
    private final Clock reloj;

    private final Set<UUID> detenciones = ConcurrentHashMap.newKeySet();
    private volatile UUID enCurso;

    public JUnitEjecutorPruebasAdapter(RegistroCasosQA registro, QaPropiedades propiedades, String iaUrl,
                                       String urlBaseDatos, Clock reloj) {
        this.registro = registro;
        this.propiedades = propiedades;
        this.iaUrl = iaUrl;
        this.urlBaseDatos = urlBaseDatos;
        this.reloj = reloj;
    }

    @Override
    public FinEjecucion ejecutar(PlanEjecucion plan, OyenteEjecucion oyente) {
        UUID id = plan.idEjecucion();
        enCurso = id;
        SesionEjecucion sesion = new SesionEjecucion(plan, entorno(), reloj);
        SesionEjecucion.Registro.agregar(sesion);
        try {
            String bloqueo = motivoBloqueoBaseReal();
            Launcher launcher = LauncherFactory.create();
            FinEjecucion fin = FinEjecucion.COMPLETA;

            for (String codigo : plan.codigos()) {
                if (detenciones.contains(id)) {
                    oyente.casoNoEjecutado(codigo, DETENIDA);
                    fin = FinEjecucion.DETENIDA;
                } else if (bloqueo != null) {
                    oyente.casoNoEjecutado(codigo, bloqueo);
                } else {
                    var metodo = registro.metodo(codigo);
                    if (metodo.isEmpty()) {
                        oyente.casoNoEjecutado(codigo, NO_IMPLEMENTADO);
                    } else {
                        correrCaso(launcher, sesion, codigo, metodo.get(), oyente);
                    }
                }
            }
            return fin;
        } finally {
            sesion.cerrarNavegador();
            SesionEjecucion.Registro.quitar(id);
            detenciones.remove(id);
            enCurso = null;
        }
    }

    private void correrCaso(Launcher launcher, SesionEjecucion sesion, String codigo, Method metodo,
                            OyenteEjecucion oyente) {
        BitacoraCaso bitacora = sesion.empezarCaso(codigo);
        OyenteJUnit escucha = new OyenteJUnit();

        LauncherDiscoveryRequest pedido = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectMethod(metodo.getDeclaringClass(), metodo))
                .configurationParameter(ExtensionQA.PARAMETRO_EJECUCION, sesion.idEjecucion().toString())
                .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "false")
                .build();

        OffsetDateTime antes = OffsetDateTime.now(reloj);
        log.info("QA {}: ejecutando {}", sesion.idEjecucion(), codigo);
        try {
            launcher.execute(pedido, escucha);
        } finally {
            sesion.cerrarNavegadorDelCaso();
        }

        // El oyente del caso de uso se llama fuera de JUnit: un error al guardar no rompe el launcher
        OffsetDateTime inicio = escucha.inicio != null ? escucha.inicio : antes;
        OffsetDateTime fin = escucha.fin != null ? escucha.fin : OffsetDateTime.now(reloj);
        if (fin.isBefore(inicio)) {
            fin = inicio;
        }

        if (escucha.motivoOmitido != null) {
            oyente.casoNoEjecutado(codigo, "JUnit no lo ejecutó: " + escucha.motivoOmitido);
            return;
        }
        TestExecutionResult resultado = escucha.resultadoMetodo != null ? escucha.resultadoMetodo : escucha.fallaContenedor;
        if (resultado == null) {
            oyente.casoNoEjecutado(codigo, "JUnit no encontró la prueba " + metodo.getDeclaringClass().getSimpleName()
                    + "." + metodo.getName() + " (revise su firma)");
            return;
        }

        Throwable error = resultado.getThrowable().orElse(null);
        switch (resultado.getStatus()) {
            case SUCCESSFUL -> oyente.casoTerminado(DesenlaceCaso.aprobado(codigo, inicio, fin,
                    bitacora.pasos(), bitacora.metricas()));
            case ABORTED -> {
                String motivo = error == null ? "Precondición no cumplida" : BitacoraCaso.resumen(error);
                if (error instanceof CasoNoEjecutableException) {
                    oyente.casoNoEjecutado(codigo, motivo);
                } else {
                    oyente.casoTerminado(DesenlaceCaso.omitido(codigo, motivo, inicio, fin, bitacora.pasos()));
                }
            }
            case FAILED -> oyente.casoTerminado(DesenlaceCaso.fallido(codigo, clasificar(error),
                    mensaje(error), bitacora.pasoFallido(), inicio, fin, bitacora.pasos(), bitacora.metricas()));
        }
    }

    /** FUNCIONAL si una asercion no se cumplio; INFRAESTRUCTURA si algo no respondio. */
    static TipoFallo clasificar(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof FalloInfraestructuraException || t instanceof ConnectException
                    || t instanceof SocketTimeoutException || t instanceof UnknownHostException
                    || t instanceof UncheckedIOException || t instanceof IOException) {
                return TipoFallo.INFRAESTRUCTURA;
            }
            if (t instanceof AssertionError) {
                return TipoFallo.FUNCIONAL;
            }
        }
        return TipoFallo.FUNCIONAL;
    }

    private static String mensaje(Throwable error) {
        if (error == null) {
            return "La prueba falló sin informar el motivo";
        }
        StringBuilder texto = new StringBuilder(BitacoraCaso.resumen(error));
        Throwable causa = error.getCause();
        if (causa != null && causa != error) {
            texto.append("\nCausa: ").append(BitacoraCaso.resumen(causa));
        }
        return texto.toString();
    }

    @Override
    public boolean detener(UUID idEjecucion) {
        // Se anota aunque todavia no haya empezado: si arranca despues, no corre ningun caso
        detenciones.add(idEjecucion);
        return idEjecucion.equals(enCurso);
    }

    @Override
    public Set<String> codigosImplementados() {
        return registro.codigos();
    }

    private EntornoQA entorno() {
        return new EntornoQA(propiedades.baseUrl(), propiedades.frontendUrl(), propiedades.usuario(),
                propiedades.contrasena(), iaUrl, CarpetaEvidencias.desde(propiedades).base());
    }

    /**
     * Las pruebas crean empresas, analisis y alertas. Para no ensuciar la base
     * real, solo corren si el backend esta conectado a una base de pruebas
     * (su nombre contiene "_qa" o empieza con "prueba"), salvo que
     * QA_PERMITIR_BASE_REAL=true.
     */
    String motivoBloqueoBaseReal() {
        if (propiedades.permitirBaseReal() || esBaseDePruebas(urlBaseDatos)) {
            return null;
        }
        return "El backend está conectado a la base real: las pruebas no se ejecutan para no ensuciarla. "
                + "Levántelo con herramientas/levantar-backend-qa.ps1 (base phishshield_qa).";
    }

    static boolean esBaseDePruebas(String urlJdbc) {
        if (urlJdbc == null) {
            return false;
        }
        String sinParametros = urlJdbc.split("\\?", 2)[0];
        String nombre = sinParametros.substring(sinParametros.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
        return nombre.contains("_qa") || nombre.startsWith("prueba");
    }

    /** Escucha a JUnit durante un caso: cuando empezo, cuando termino y como. */
    private final class OyenteJUnit implements TestExecutionListener {

        private volatile OffsetDateTime inicio;
        private volatile OffsetDateTime fin;
        private volatile TestExecutionResult resultadoMetodo;
        private volatile TestExecutionResult fallaContenedor;
        private volatile String motivoOmitido;

        @Override
        public void executionStarted(TestIdentifier prueba) {
            if (prueba.isTest()) {
                inicio = OffsetDateTime.now(reloj);
            }
        }

        @Override
        public void executionSkipped(TestIdentifier prueba, String motivo) {
            motivoOmitido = motivo == null ? "sin motivo" : motivo;
        }

        @Override
        public void executionFinished(TestIdentifier prueba, TestExecutionResult resultado) {
            if (prueba.isTest()) {
                fin = OffsetDateTime.now(reloj);
                resultadoMetodo = resultado;
            } else if (resultado.getStatus() == TestExecutionResult.Status.FAILED && fallaContenedor == null) {
                // Por ejemplo, el constructor de la clase de pruebas lanzo una excepcion
                fallaContenedor = resultado;
            }
        }
    }
}
