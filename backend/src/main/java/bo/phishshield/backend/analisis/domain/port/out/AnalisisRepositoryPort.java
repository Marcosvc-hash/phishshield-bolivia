package bo.phishshield.backend.analisis.domain.port.out;

import bo.phishshield.backend.analisis.domain.model.Analisis;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida: persistencia de analisis.
 * Guardar un analisis implica escribir tambien sus indicadores;
 * el adaptador se encarga de que sea una sola transaccion.
 */
public interface AnalisisRepositoryPort {

    Analisis guardar(Analisis analisis);

    /** Busca un analisis previo del mismo contenido, para no recalcular. */
    Optional<Analisis> buscarPorHash(String contenidoHash);

    Optional<Analisis> buscarPorUuid(UUID uuidPublico);

    List<Analisis> listarUltimos(int limite);
}