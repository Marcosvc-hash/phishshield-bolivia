package bo.phishshield.backend.historial.application;

import bo.phishshield.backend.historial.domain.model.AnalisisFila;
import bo.phishshield.backend.historial.domain.model.FiltroHistorial;
import bo.phishshield.backend.historial.domain.port.out.HistorialQueryPort;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.Pagina;
import bo.phishshield.backend.shared.domain.model.SolicitudPagina;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;

/**
 * Caso de uso: historial de analisis paginado y filtrado.
 * ADMIN ve todos (incluidos los anonimos); ADMIN_EMPRESA, solo los de su empresa.
 */
public class ListarHistorialUseCase {

    private final HistorialQueryPort consultas;

    public ListarHistorialUseCase(HistorialQueryPort consultas) {
        this.consultas = consultas;
    }

    public Pagina<AnalisisFila> ejecutar(UsuarioActual quien, FiltroHistorial filtro, SolicitudPagina pagina) {
        AlcanceDatos alcance = AlcanceDatos.administrativo(quien);
        return consultas.buscar(alcance, filtro, pagina);
    }
}
