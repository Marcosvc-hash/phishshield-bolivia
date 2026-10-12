package bo.phishshield.backend.analisis.domain.port.out;

import bo.phishshield.backend.analisis.domain.model.PrediccionIa;

import java.util.Optional;

/**
 * Consulta al modelo de IA de URL. Devuelve vacio si el modelo no esta
 * disponible: el analisis debe seguir funcionando solo con las reglas.
 */
public interface ModeloIaPort {

    Optional<PrediccionIa> predecir(String url);
}
