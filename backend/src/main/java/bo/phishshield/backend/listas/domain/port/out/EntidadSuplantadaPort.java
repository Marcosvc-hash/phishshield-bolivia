package bo.phishshield.backend.listas.domain.port.out;

import bo.phishshield.backend.listas.domain.model.EntidadSuplantada;

import java.util.List;
import java.util.Optional;

/** Lectura de entidades_suplantadas para asociarlas a un dominio. */
public interface EntidadSuplantadaPort {

    List<EntidadSuplantada> listar();

    /** El id interno de la entidad con ese nombre exacto. */
    Optional<Long> idPorNombre(String nombre);
}
