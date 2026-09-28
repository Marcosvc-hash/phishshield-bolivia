package bo.phishshield.backend.analisis.domain.port.out;

import bo.phishshield.backend.analisis.domain.model.DominioOficial;

import java.util.List;

/**
 * Puerto de salida: catalogo de entidades bolivianas suplantadas
 * con sus dominios legitimos.
 */
public interface DominioOficialRepositoryPort {

    List<DominioOficial> listarActivos();
}