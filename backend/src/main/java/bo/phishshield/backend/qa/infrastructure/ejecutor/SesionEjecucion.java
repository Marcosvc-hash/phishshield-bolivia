package bo.phishshield.backend.qa.infrastructure.ejecutor;

import bo.phishshield.backend.qa.domain.model.PlanEjecucion;
import bo.phishshield.backend.qa.domain.port.out.LoteRobustezPort;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

/**
 * Estado compartido por los casos de una misma ejecucion: el plan, el
 * entorno, el caso que esta corriendo y el token del administrador de
 * prueba (se reutiliza entre casos y se renueva antes de que venza: el JWT
 * dura 15 minutos y una ejecucion larga puede superar ese tiempo).
 *
 * JUnit crea las clases de prueba por su cuenta (no son beans de Spring);
 * por eso los casos llegan a esta sesion a traves de un registro estatico
 * (SesionesQA) y de la ExtensionQA.
 */
public final class SesionEjecucion {

    private final PlanEjecucion plan;
    private final EntornoQA entorno;
    private final Clock reloj;
    private volatile BitacoraCaso casoActual;

    private String token;
    private Instant tokenVence;

    private Playwright playwright;
    private Browser navegador;
    private NavegadorQA navegadorCaso;

    private final LoteRobustezPort lote;
    private final BooleanSupplier detenida;

    public SesionEjecucion(PlanEjecucion plan, EntornoQA entorno, Clock reloj) {
        this(plan, entorno, reloj, List::of, () -> false);
    }

    public SesionEjecucion(PlanEjecucion plan, EntornoQA entorno, Clock reloj,
                           LoteRobustezPort lote, BooleanSupplier detenida) {
        this.plan = plan;
        this.entorno = entorno;
        this.reloj = reloj;
        this.lote = lote;
        this.detenida = detenida;
    }

    LoteRobustezPort lote() {
        return lote;
    }

    /** true si alguien pidio detener esta ejecucion (las pruebas largas lo consultan). */
    boolean detenida() {
        return detenida.getAsBoolean();
    }

    public UUID idEjecucion() {
        return plan.idEjecucion();
    }

    public PlanEjecucion plan() {
        return plan;
    }

    public EntornoQA entorno() {
        return entorno;
    }

    public Clock reloj() {
        return reloj;
    }

    BitacoraCaso empezarCaso(String codigo) {
        casoActual = new BitacoraCaso(codigo, reloj);
        return casoActual;
    }

    // ------------------------------------------------------------------
    // Navegador (casos de interfaz): un Chromium por ejecucion, una
    // pestana aislada por caso. Solo se abre si algun caso lo pide.
    // ------------------------------------------------------------------

    /** La pestana del caso actual; la primera vez abre Chromium en modo headless. */
    synchronized NavegadorQA navegadorDelCaso() {
        if (navegadorCaso == null) {
            navegadorCaso = new NavegadorQA(chromium(), entorno.carpetaEvidencias(),
                    entorno.carpetaEvidencias().resolve(idEjecucion().toString()), casoActual.codigo());
        }
        return navegadorCaso;
    }

    /** Cierra la pestana del caso que termino (el navegador sigue abierto para el siguiente). */
    synchronized void cerrarNavegadorDelCaso() {
        if (navegadorCaso != null) {
            navegadorCaso.close();
            navegadorCaso = null;
        }
    }

    private Browser chromium() {
        if (navegador == null) {
            // Sin esta variable, Playwright intentaria descargar Firefox y WebKit al arrancar
            playwright = Playwright.create(new Playwright.CreateOptions()
                    .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
            try {
                navegador = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            } catch (PlaywrightException ex) {
                cerrarNavegador();
                if (String.valueOf(ex.getMessage()).contains("Executable doesn't exist")) {
                    throw new CasoNoEjecutableException("Chromium no está instalado para Playwright. Desde backend\\: "
                            + "java -cp (Get-Content target\\cp.txt) com.microsoft.playwright.CLI install chromium");
                }
                throw new FalloInfraestructuraException("No se pudo abrir Chromium: " + ex.getMessage(), ex);
            }
        }
        return navegador;
    }

    /** Al terminar la ejecucion. */
    synchronized void cerrarNavegador() {
        cerrarNavegadorDelCaso();
        try {
            if (navegador != null) {
                navegador.close();
            }
            if (playwright != null) {
                playwright.close();
            }
        } catch (RuntimeException ignorada) {
            // Si Chromium ya se cerro solo, no hay nada que liberar
        } finally {
            navegador = null;
            playwright = null;
        }
    }

    BitacoraCaso casoActual() {
        return casoActual;
    }

    /** El token guardado si le queda al menos un minuto de vida; si no, null. */
    synchronized String tokenVigente() {
        if (token == null || tokenVence == null || Instant.now(reloj).plusSeconds(60).isAfter(tokenVence)) {
            return null;
        }
        return token;
    }

    synchronized void guardarToken(String token, Instant vence) {
        this.token = token;
        this.tokenVence = vence;
    }

    synchronized Instant tokenVence() {
        return tokenVence;
    }

    /** Registro de las sesiones activas, por id de ejecucion. Como maximo hay una. */
    public static final class Registro {

        private static final Map<UUID, SesionEjecucion> ACTIVAS = new ConcurrentHashMap<>();

        private Registro() {
        }

        static void agregar(SesionEjecucion sesion) {
            ACTIVAS.put(sesion.idEjecucion(), sesion);
        }

        static void quitar(UUID id) {
            ACTIVAS.remove(id);
        }

        static SesionEjecucion buscar(UUID id) {
            return ACTIVAS.get(id);
        }
    }
}
