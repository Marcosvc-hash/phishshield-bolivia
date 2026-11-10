package bo.phishshield.backend.listas.application;

import bo.phishshield.backend.listas.domain.model.DominioEnLista;
import bo.phishshield.backend.listas.domain.model.DominioFila;
import bo.phishshield.backend.listas.domain.model.DominioNoEncontradoException;
import bo.phishshield.backend.listas.domain.port.out.DominioListaRepositoryPort;
import bo.phishshield.backend.listas.domain.port.out.EntidadSuplantadaPort;
import bo.phishshield.backend.shared.domain.model.AccionAuditoria;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import bo.phishshield.backend.shared.domain.port.out.AuditoriaPort;

import java.util.UUID;

/**
 * Caso de uso: corregir el motivo o la entidad de un dominio (solo ADMIN).
 * El dominio y la lista no se editan: para eso se desactiva y se agrega otro.
 */
public class EditarDominioUseCase {

    private final DominioListaRepositoryPort dominios;
    private final CambiosDeListas cambios;

    public EditarDominioUseCase(DominioListaRepositoryPort dominios, EntidadSuplantadaPort entidades,
                                AuditoriaPort auditoria) {
        this.dominios = dominios;
        this.cambios = new CambiosDeListas(dominios, entidades, auditoria);
    }

    public DominioFila ejecutar(UsuarioActual quien, UUID id, ComandoDominio comando) {
        AlcanceDatos.soloPlataforma(quien);

        DominioEnLista dominio = dominios.buscarPorUuid(id)
                .orElseThrow(() -> new DominioNoEncontradoException(id));
        dominio.editar(comando.motivo(), cambios.idEntidad(comando.entidad()));

        DominioEnLista guardado = dominios.guardar(dominio);
        cambios.registrar(AccionAuditoria.DOMINIO_EDITADO, quien, guardado, comando.entidad(), comando.ip());

        return dominios.buscarFila(id).orElseThrow(() -> new DominioNoEncontradoException(id));
    }
}
