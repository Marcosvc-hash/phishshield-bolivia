package bo.phishshield.backend.historial.domain.port.out;

import bo.phishshield.backend.historial.domain.model.AnalisisFila;
import bo.phishshield.backend.historial.domain.model.DetalleAnalisis;
import bo.phishshield.backend.historial.domain.model.FiltroHistorial;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.Pagina;
import bo.phishshield.backend.shared.domain.model.SolicitudPagina;

import java.util.Optional;
import java.util.UUID;

/** Consultas de solo lectura sobre los analisis guardados. */
public interface HistorialQueryPort {

    /**
     * Una pagina de analisis, del mas nuevo al mas viejo. El filtro por
     * empresa va en el SQL: si se filtrara despues, las paginas saldrian
     * incompletas y el total no cuadraria.
     */
    Pagina<AnalisisFila> buscar(AlcanceDatos alcance, FiltroHistorial filtro, SolicitudPagina pagina);

    /** El detalle sin filtrar por empresa: el caso de uso revisa el alcance. */
    Optional<DetalleAnalisis> detalle(UUID id);
}
