package bo.phishshield.backend.qa.application;

import bo.phishshield.backend.qa.domain.model.Ejecucion;
import bo.phishshield.backend.qa.domain.model.EjecucionNoEncontradaException;
import bo.phishshield.backend.qa.domain.port.out.EjecucionRepositoryPort;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;

import java.util.UUID;

/** Caso de uso: una ejecucion con sus casos, pasos y metricas. Solo ADMIN. */
public class ObtenerDetalleEjecucionUseCase {

    private final EjecucionRepositoryPort ejecuciones;

    public ObtenerDetalleEjecucionUseCase(EjecucionRepositoryPort ejecuciones) {
        this.ejecuciones = ejecuciones;
    }

    public Ejecucion ejecutar(UsuarioActual quien, UUID id) {
        AlcanceDatos.soloPlataforma(quien);
        return ejecuciones.buscarPorUuid(id).orElseThrow(() -> new EjecucionNoEncontradaException(id));
    }
}
