package bo.phishshield.backend.qa.domain.port.out;

import bo.phishshield.backend.qa.domain.model.Ejecucion;
import bo.phishshield.backend.qa.domain.model.FilaEjecucion;
import bo.phishshield.backend.shared.domain.model.Pagina;
import bo.phishshield.backend.shared.domain.model.SolicitudPagina;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Guarda y lee las ejecuciones con sus resultados, pasos y metricas. */
public interface EjecucionRepositoryPort {

    /** Inserta si no tiene id; si lo tiene, actualiza la ejecucion y sus resultados. */
    Ejecucion guardar(Ejecucion ejecucion);

    Optional<Ejecucion> buscarPorUuid(UUID uuidPublico);

    /** Historial, de la mas reciente a la mas antigua. */
    Pagina<FilaEjecucion> listar(SolicitudPagina solicitud);

    /** La ejecucion pendiente o en curso, si hay una (como maximo hay una: uq_qa_ejec_activa). */
    Optional<UUID> buscarActiva();

    /**
     * Ejecuciones que siguen pendientes o en curso. Al arrancar el backend,
     * cualquiera que aparezca aqui quedo interrumpida por un reinicio.
     */
    List<Ejecucion> listarActivas();
}
