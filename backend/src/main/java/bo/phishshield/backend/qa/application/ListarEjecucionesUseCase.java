package bo.phishshield.backend.qa.application;

import bo.phishshield.backend.qa.domain.model.FilaEjecucion;
import bo.phishshield.backend.qa.domain.port.out.EjecucionRepositoryPort;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.Pagina;
import bo.phishshield.backend.shared.domain.model.SolicitudPagina;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;

/** Caso de uso: historial de ejecuciones, de la mas reciente a la mas antigua. Solo ADMIN. */
public class ListarEjecucionesUseCase {

    private final EjecucionRepositoryPort ejecuciones;

    public ListarEjecucionesUseCase(EjecucionRepositoryPort ejecuciones) {
        this.ejecuciones = ejecuciones;
    }

    public Pagina<FilaEjecucion> ejecutar(UsuarioActual quien, SolicitudPagina pagina) {
        AlcanceDatos.soloPlataforma(quien);
        return ejecuciones.listar(pagina);
    }
}
