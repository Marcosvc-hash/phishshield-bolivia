package bo.phishshield.backend.listas.application;

import bo.phishshield.backend.listas.domain.model.ConflictoListaException;
import bo.phishshield.backend.listas.domain.model.DominioEnLista;
import bo.phishshield.backend.listas.domain.model.NombreDominio;
import bo.phishshield.backend.listas.domain.model.TipoLista;
import bo.phishshield.backend.listas.domain.port.out.DominioListaRepositoryPort;
import bo.phishshield.backend.listas.domain.port.out.EntidadSuplantadaPort;
import bo.phishshield.backend.shared.domain.model.AccionAuditoria;
import bo.phishshield.backend.shared.domain.model.EventoAuditoria;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import bo.phishshield.backend.shared.domain.port.out.AuditoriaPort;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pasos que repiten los casos de uso de listas: traducir el nombre de la
 * entidad a su id, revisar que no choque con la lista opuesta y dejar
 * constancia en auditoria. Clase de paquete: no es un caso de uso.
 *
 * La auditoria cumple dos funciones: el registro de accesos y la cache del
 * motor (AnalizarUrlUseCase deja de reutilizar analisis anteriores al
 * ultimo DOMINIO_*).
 */
final class CambiosDeListas {

    private final DominioListaRepositoryPort dominios;
    private final EntidadSuplantadaPort entidades;
    private final AuditoriaPort auditoria;

    CambiosDeListas(DominioListaRepositoryPort dominios, EntidadSuplantadaPort entidades, AuditoriaPort auditoria) {
        this.dominios = dominios;
        this.entidades = entidades;
        this.auditoria = auditoria;
    }

    Long idEntidad(String nombre) {
        if (nombre == null) {
            return null;
        }
        return entidades.idPorNombre(nombre)
                .orElseThrow(() -> new IllegalArgumentException("No existe la entidad suplantada: " + nombre));
    }

    /** Un dominio no puede estar activo en la blanca y en la negra a la vez. */
    void validarSinChoque(NombreDominio dominio, TipoLista lista) {
        TipoLista opuesta = lista.opuesta();
        if (opuesta == null) {
            return;
        }
        if (dominios.buscar(dominio, opuesta).filter(DominioEnLista::isActivo).isPresent()) {
            throw new ConflictoListaException(dominio + " esta activo en la lista " + opuesta.aValorBd()
                    + ". Desactivelo alli primero: no puede ser legitimo y malicioso a la vez.");
        }
    }

    void registrar(AccionAuditoria accion, UsuarioActual quien, DominioEnLista dominio, String entidad, String ip) {
        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("dominio", dominio.getDominio().valor());
        detalle.put("lista", dominio.getLista().aValorBd());
        detalle.put("motivo", dominio.getMotivo());
        detalle.put("entidad", entidad);
        auditoria.registrar(EventoAuditoria.de(accion, quien, "dominios_lista", dominio.getId(), detalle, ip));
    }
}
