package bo.phishshield.backend.listas.application;

import bo.phishshield.backend.listas.domain.model.ConflictoListaException;
import bo.phishshield.backend.listas.domain.model.DominioEnLista;
import bo.phishshield.backend.listas.domain.model.DominioFila;
import bo.phishshield.backend.listas.domain.model.DominioNoEncontradoException;
import bo.phishshield.backend.listas.domain.model.NombreDominio;
import bo.phishshield.backend.listas.domain.model.TipoLista;
import bo.phishshield.backend.listas.domain.port.out.DominioListaRepositoryPort;
import bo.phishshield.backend.listas.domain.port.out.EntidadSuplantadaPort;
import bo.phishshield.backend.shared.domain.model.AccionAuditoria;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import bo.phishshield.backend.shared.domain.port.out.AuditoriaPort;

import java.util.Optional;

/**
 * Caso de uso: agregar un dominio a la lista blanca o negra (solo ADMIN).
 *
 * Si ese dominio ya estuvo en esa lista y se desactivo, se reactiva la
 * misma fila con el motivo nuevo: la tabla tiene UNIQUE(dominio, lista) y
 * asi se conserva su historia.
 */
public class AgregarDominioUseCase {

    private final DominioListaRepositoryPort dominios;
    private final CambiosDeListas cambios;

    public AgregarDominioUseCase(DominioListaRepositoryPort dominios, EntidadSuplantadaPort entidades,
                                 AuditoriaPort auditoria) {
        this.dominios = dominios;
        this.cambios = new CambiosDeListas(dominios, entidades, auditoria);
    }

    public DominioFila ejecutar(UsuarioActual quien, ComandoDominio comando) {
        AlcanceDatos.soloPlataforma(quien);

        NombreDominio dominio = NombreDominio.de(comando.dominio());
        TipoLista lista = TipoLista.desdeValorBd(comando.lista());
        Long idEntidad = cambios.idEntidad(comando.entidad());
        cambios.validarSinChoque(dominio, lista);

        Optional<DominioEnLista> existente = dominios.buscar(dominio, lista);
        if (existente.filter(DominioEnLista::isActivo).isPresent()) {
            throw new ConflictoListaException(dominio + " ya esta en la lista " + lista.aValorBd());
        }

        DominioEnLista aGuardar;
        AccionAuditoria accion;
        if (existente.isPresent()) {
            aGuardar = existente.get();
            aGuardar.editar(comando.motivo(), idEntidad);
            aGuardar.reactivar();
            accion = AccionAuditoria.DOMINIO_REACTIVADO;
        } else {
            aGuardar = DominioEnLista.nuevo(dominio, lista, comando.motivo(), idEntidad);
            accion = AccionAuditoria.DOMINIO_AGREGADO;
        }

        DominioEnLista guardado = dominios.guardar(aGuardar);
        cambios.registrar(accion, quien, guardado, comando.entidad(), comando.ip());

        return dominios.buscarFila(guardado.getUuidPublico())
                .orElseThrow(() -> new DominioNoEncontradoException(guardado.getUuidPublico()));
    }
}
