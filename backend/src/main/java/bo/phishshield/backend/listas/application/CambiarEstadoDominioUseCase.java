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
 * Caso de uso: "eliminar" (desactivar) o reactivar un dominio (solo ADMIN).
 * Nunca se borra la fila. Si ya esta en el estado pedido, no hace nada.
 */
public class CambiarEstadoDominioUseCase {

    private final DominioListaRepositoryPort dominios;
    private final CambiosDeListas cambios;

    public CambiarEstadoDominioUseCase(DominioListaRepositoryPort dominios, EntidadSuplantadaPort entidades,
                                       AuditoriaPort auditoria) {
        this.dominios = dominios;
        this.cambios = new CambiosDeListas(dominios, entidades, auditoria);
    }

    public DominioFila ejecutar(UsuarioActual quien, UUID id, boolean activar, String ip) {
        AlcanceDatos.soloPlataforma(quien);

        DominioEnLista dominio = dominios.buscarPorUuid(id)
                .orElseThrow(() -> new DominioNoEncontradoException(id));

        if (dominio.isActivo() != activar) {
            if (activar) {
                cambios.validarSinChoque(dominio.getDominio(), dominio.getLista());
                dominio.reactivar();
            } else {
                dominio.desactivar();
            }
            DominioEnLista guardado = dominios.guardar(dominio);
            cambios.registrar(activar ? AccionAuditoria.DOMINIO_REACTIVADO : AccionAuditoria.DOMINIO_DESACTIVADO,
                    quien, guardado, null, ip);
        }

        return dominios.buscarFila(id).orElseThrow(() -> new DominioNoEncontradoException(id));
    }
}
