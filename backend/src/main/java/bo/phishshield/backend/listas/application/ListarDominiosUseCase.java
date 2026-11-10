package bo.phishshield.backend.listas.application;

import bo.phishshield.backend.listas.domain.model.DominioFila;
import bo.phishshield.backend.listas.domain.model.EntidadSuplantada;
import bo.phishshield.backend.listas.domain.model.FiltroDominios;
import bo.phishshield.backend.listas.domain.port.out.DominioListaRepositoryPort;
import bo.phishshield.backend.listas.domain.port.out.EntidadSuplantadaPort;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.Pagina;
import bo.phishshield.backend.shared.domain.model.SolicitudPagina;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;

import java.util.List;

/**
 * Caso de uso: consultar las listas de dominios y las entidades
 * suplantadas (para el selector). Solo ADMIN.
 */
public class ListarDominiosUseCase {

    private final DominioListaRepositoryPort dominios;
    private final EntidadSuplantadaPort entidades;

    public ListarDominiosUseCase(DominioListaRepositoryPort dominios, EntidadSuplantadaPort entidades) {
        this.dominios = dominios;
        this.entidades = entidades;
    }

    public Pagina<DominioFila> ejecutar(UsuarioActual quien, FiltroDominios filtro, SolicitudPagina pagina) {
        AlcanceDatos.soloPlataforma(quien);
        return dominios.buscar(filtro, pagina);
    }

    public List<EntidadSuplantada> entidades(UsuarioActual quien) {
        AlcanceDatos.soloPlataforma(quien);
        return entidades.listar();
    }
}
