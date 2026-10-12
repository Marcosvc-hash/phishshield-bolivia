package bo.phishshield.backend.analisis.domain.port.out;

import bo.phishshield.backend.analisis.domain.model.DominioListado;

import java.util.List;

/**
 * Puerto de salida: dominios verificados como legitimos.
 * Sirve para que una heuristica no acuse a un sitio conocido
 * (por ejemplo visa.com, que se parece a bisa.com).
 */
public interface ListaBlancaRepositoryPort {

    List<DominioListado> listarActivos();
}
