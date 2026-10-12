package bo.phishshield.backend.analisis.domain.port.out;

import java.util.Optional;

/** Busca en modelos_ia el registro del modelo que respondio, para guardarlo en analisis.id_modelo. */
public interface ModeloIaRepositoryPort {

    Optional<Long> buscarId(String nombre, String version);
}
