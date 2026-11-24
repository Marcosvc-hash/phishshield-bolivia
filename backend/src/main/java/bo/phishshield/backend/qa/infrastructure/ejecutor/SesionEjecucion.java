package bo.phishshield.backend.qa.infrastructure.ejecutor;

import bo.phishshield.backend.qa.domain.model.PlanEjecucion;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    public SesionEjecucion(PlanEjecucion plan, EntornoQA entorno, Clock reloj) {
        this.plan = plan;
        this.entorno = entorno;
        this.reloj = reloj;
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
