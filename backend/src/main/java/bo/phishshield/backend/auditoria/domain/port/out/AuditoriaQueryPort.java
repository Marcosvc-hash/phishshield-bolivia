package bo.phishshield.backend.auditoria.domain.port.out;

import bo.phishshield.backend.auditoria.domain.model.FiltroAuditoria;
import bo.phishshield.backend.auditoria.domain.model.RegistroAuditoria;
import bo.phishshield.backend.shared.domain.model.Pagina;
import bo.phishshield.backend.shared.domain.model.SolicitudPagina;

import java.util.List;

/** Lectura de la tabla auditoria (la escritura va por shared AuditoriaPort). */
public interface AuditoriaQueryPort {

    /** Del registro mas nuevo al mas viejo. */
    Pagina<RegistroAuditoria> buscar(FiltroAuditoria filtro, SolicitudPagina pagina);

    /** Las acciones que aparecen en la tabla, para el selector del filtro. */
    List<String> acciones();
}
