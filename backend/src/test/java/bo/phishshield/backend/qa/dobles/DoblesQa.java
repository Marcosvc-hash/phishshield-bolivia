package bo.phishshield.backend.qa.dobles;

import bo.phishshield.backend.qa.domain.model.CasoPrueba;
import bo.phishshield.backend.qa.domain.model.DesenlaceCaso;
import bo.phishshield.backend.qa.domain.model.Ejecucion;
import bo.phishshield.backend.qa.domain.model.EjecucionEnCursoException;
import bo.phishshield.backend.qa.domain.model.FilaEjecucion;
import bo.phishshield.backend.qa.domain.model.FinEjecucion;
import bo.phishshield.backend.qa.domain.model.Herramienta;
import bo.phishshield.backend.qa.domain.model.PlanEjecucion;
import bo.phishshield.backend.qa.domain.model.ResultadoCaso;
import bo.phishshield.backend.qa.domain.model.Suite;
import bo.phishshield.backend.qa.domain.model.TransicionEjecucionInvalidaException;
import bo.phishshield.backend.qa.domain.port.out.CatalogoCasosPort;
import bo.phishshield.backend.qa.domain.port.out.EjecucionRepositoryPort;
import bo.phishshield.backend.qa.domain.port.out.EjecutorPruebasPort;
import bo.phishshield.backend.qa.domain.port.out.OyenteEjecucion;
import bo.phishshield.backend.qa.domain.port.out.PlanificadorEjecucionPort;
import bo.phishshield.backend.shared.domain.model.Pagina;
import bo.phishshield.backend.shared.domain.model.SolicitudPagina;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

/** Dobles en memoria de los puertos de la vertical qa, para probar los casos de uso sin Spring. */
public final class DoblesQa {

    private DoblesQa() {
    }

    public static CasoPrueba caso(String codigo, Suite suite, int orden) {
        return new CasoPrueba((long) orden, codigo, suite, Herramienta.API, orden, "Caso " + codigo,
                "objetivo", "precondiciones", "datos", "esperado", true);
    }

    /** Catalogo fijo. */
    public static class CatalogoEnMemoria implements CatalogoCasosPort {
        public final List<CasoPrueba> casos = new ArrayList<>();

        public CatalogoEnMemoria(CasoPrueba... casos) {
            this.casos.addAll(List.of(casos));
        }

        @Override
        public List<CasoPrueba> listarActivos() {
            return casos.stream().filter(CasoPrueba::activo).toList();
        }

        @Override
        public List<CasoPrueba> listarPorSuite(Suite suite) {
            return casos.stream().filter(c -> c.activo() && c.suite() == suite)
                    .sorted(Comparator.comparingInt(CasoPrueba::orden)).toList();
        }

        @Override
        public List<CasoPrueba> buscarPorCodigos(Collection<String> codigos) {
            return casos.stream().filter(c -> codigos.contains(c.codigo())).toList();
        }
    }

    /**
     * Imita al adaptador JDBC: asigna id al insertar, guarda una COPIA (como
     * la base) y no deja que una ejecucion terminada vuelva a estar activa.
     */
    public static class EjecucionesEnMemoria implements EjecucionRepositoryPort {
        public final Map<UUID, Ejecucion> enBase = new LinkedHashMap<>();
        public int guardados;
        private long siguienteId = 1;

        @Override
        public Ejecucion guardar(Ejecucion e) {
            guardados++;
            if (e.getId() == null) {
                if (buscarActiva().isPresent()) {
                    throw new EjecucionEnCursoException(buscarActiva().get());
                }
                Ejecucion conId = copiar(e, siguienteId++);
                enBase.put(e.getUuidPublico(), conId);
                return copiar(conId, conId.getId());
            }
            Ejecucion actual = enBase.get(e.getUuidPublico());
            if (actual.getEstado().esFinal() && actual.getEstado() != e.getEstado()) {
                throw new TransicionEjecucionInvalidaException("ya terminó en la base");
            }
            enBase.put(e.getUuidPublico(), copiar(e, e.getId()));
            return e;
        }

        @Override
        public Optional<Ejecucion> buscarPorUuid(UUID uuid) {
            Ejecucion e = enBase.get(uuid);
            return e == null ? Optional.empty() : Optional.of(copiar(e, e.getId()));
        }

        @Override
        public Pagina<FilaEjecucion> listar(SolicitudPagina solicitud) {
            List<FilaEjecucion> filas = enBase.values().stream()
                    .map(e -> new FilaEjecucion(e.getUuidPublico(), e.getSuite(), e.getAlcance(), e.getCorreo().valor(),
                            e.getEstado(), e.getResultadoGlobal(), e.getCreadaEn(), e.getInicio(), e.getFin(),
                            e.resumen(), e.getEstadoCorreo()))
                    .toList();
            return Pagina.de(filas, solicitud, filas.size());
        }

        @Override
        public Optional<UUID> buscarActiva() {
            return enBase.values().stream().filter(e -> e.getEstado().estaActiva())
                    .map(Ejecucion::getUuidPublico).findFirst();
        }

        @Override
        public List<Ejecucion> listarActivas() {
            return enBase.values().stream().filter(e -> e.getEstado().estaActiva())
                    .map(e -> copiar(e, e.getId())).toList();
        }

        public Ejecucion unica() {
            return enBase.values().iterator().next();
        }

        private static Ejecucion copiar(Ejecucion e, Long id) {
            List<ResultadoCaso> resultados = e.getResultados().stream()
                    .map(r -> new ResultadoCaso((long) r.getOrden(), r.getCaso(), r.getOrden(), r.getEstado(),
                            r.getTipoFallo(), r.getMensajeError(), r.getPasoFallido(), r.getObservacion(),
                            r.getInicio(), r.getFin(), r.getPasos(), r.getMetricas()))
                    .toList();
            return new Ejecucion(id, e.getUuidPublico(), e.getIdUsuario(), e.getSuite(), e.getAlcance(), e.getCorreo(),
                    e.getAmbiente(), e.getParametros(), e.getEstado(), e.getResultadoGlobal(), e.getCreadaEn(),
                    e.getInicio(), e.getFin(), e.getMensajeError(), e.getEstadoCorreo(), e.getErrorCorreo(),
                    e.getCorreoEnviadoEn(), e.getRutaReportePdf(), e.getRutaReporteHtml(), resultados);
        }
    }

    /** Anota que ejecuciones se programaron, sin correrlas. */
    public static class PlanificadorQueAnota implements PlanificadorEjecucionPort {
        public final List<UUID> programadas = new ArrayList<>();

        @Override
        public void programar(UUID idEjecucion) {
            programadas.add(idEjecucion);
        }
    }

    /**
     * Ejecutor guionado: para cada plan llama al guion con el oyente. Si
     * "corriendo" es true, detener() responde que la ejecucion esta corriendo.
     */
    public static class EjecutorGuionado implements EjecutorPruebasPort {
        public BiConsumer<PlanEjecucion, OyenteEjecucion> guion = (plan, oyente) -> { };
        public FinEjecucion fin = FinEjecucion.COMPLETA;
        public RuntimeException error;
        public boolean corriendo;
        public final List<UUID> detenidas = new ArrayList<>();
        public Set<String> implementados = Set.of();

        @Override
        public FinEjecucion ejecutar(PlanEjecucion plan, OyenteEjecucion oyente) {
            guion.accept(plan, oyente);
            if (error != null) {
                throw error;
            }
            return fin;
        }

        @Override
        public boolean detener(UUID idEjecucion) {
            detenidas.add(idEjecucion);
            return corriendo;
        }

        @Override
        public Set<String> codigosImplementados() {
            return implementados;
        }
    }

    /** Atajo para guiones: todos los casos del plan terminan en PASS. */
    public static BiConsumer<PlanEjecucion, OyenteEjecucion> todosPass(java.time.OffsetDateTime cuando) {
        return (plan, oyente) -> plan.codigos().forEach(c ->
                oyente.casoTerminado(DesenlaceCaso.aprobado(c, cuando, cuando.plusSeconds(1), List.of(), List.of())));
    }
}
