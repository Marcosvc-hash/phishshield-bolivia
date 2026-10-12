package bo.phishshield.backend.analisis.domain.port.out;

import bo.phishshield.backend.analisis.domain.model.DominioListado;

import java.util.List;

/**
 * Puerto de salida: dominios reportados como maliciosos.
 * El dominio no sabe que detras hay una tabla dominios_lista
 * con listas blanca, negra y gris.
 */
public interface ListaNegraRepositoryPort {

    List<DominioListado> listarActivos();
}
