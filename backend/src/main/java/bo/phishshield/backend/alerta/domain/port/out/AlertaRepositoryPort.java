package bo.phishshield.backend.alerta.domain.port.out;

import bo.phishshield.backend.alerta.domain.model.AlertaFila;
import bo.phishshield.backend.alerta.domain.model.AlertaGestionada;
import bo.phishshield.backend.alerta.domain.model.EstadoAlerta;
import bo.phishshield.backend.alerta.domain.model.FiltroAlertas;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.Pagina;
import bo.phishshield.backend.shared.domain.model.SolicitudPagina;

import java.util.Optional;
import java.util.UUID;

/** Puerto de salida de la bandeja de alertas. */
public interface AlertaRepositoryPort {

    /** Abiertas primero (pendiente, en_revision), luego por fecha, la mas nueva arriba. */
    Pagina<AlertaFila> buscar(AlcanceDatos alcance, FiltroAlertas filtro, SolicitudPagina pagina);

    long contarPendientes(AlcanceDatos alcance);

    /** Sin filtrar por empresa: el caso de uso revisa el alcance. */
    Optional<AlertaGestionada> buscarPorUuid(UUID id);

    Optional<AlertaFila> buscarFila(UUID id);

    /**
     * Guarda estado, asignado, notas y fecha de cierre, pero solo si en la
     * base la alerta sigue en estadoLeido. Devuelve false si otro la cambio
     * antes (control de concurrencia optimista).
     */
    boolean actualizar(AlertaGestionada alerta, EstadoAlerta estadoLeido);
}
