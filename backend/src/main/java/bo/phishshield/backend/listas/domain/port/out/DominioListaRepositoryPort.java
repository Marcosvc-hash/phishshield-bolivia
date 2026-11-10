package bo.phishshield.backend.listas.domain.port.out;

import bo.phishshield.backend.listas.domain.model.DominioEnLista;
import bo.phishshield.backend.listas.domain.model.DominioFila;
import bo.phishshield.backend.listas.domain.model.FiltroDominios;
import bo.phishshield.backend.listas.domain.model.NombreDominio;
import bo.phishshield.backend.listas.domain.model.TipoLista;
import bo.phishshield.backend.shared.domain.model.Pagina;
import bo.phishshield.backend.shared.domain.model.SolicitudPagina;

import java.util.Optional;
import java.util.UUID;

/** Puerto de salida de la gestion de listas (tabla dominios_lista). */
public interface DominioListaRepositoryPort {

    Pagina<DominioFila> buscar(FiltroDominios filtro, SolicitudPagina pagina);

    Optional<DominioEnLista> buscarPorUuid(UUID id);

    /** La fila de ese dominio en esa lista, activa o no (UNIQUE(dominio, lista)). */
    Optional<DominioEnLista> buscar(NombreDominio dominio, TipoLista lista);

    /** Inserta si no tiene id; si lo tiene, actualiza motivo, entidad y activo. Nunca borra. */
    DominioEnLista guardar(DominioEnLista dominio);

    Optional<DominioFila> buscarFila(UUID id);
}
