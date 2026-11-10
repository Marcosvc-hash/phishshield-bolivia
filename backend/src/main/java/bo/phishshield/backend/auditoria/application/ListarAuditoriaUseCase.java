package bo.phishshield.backend.auditoria.application;

import bo.phishshield.backend.auditoria.domain.model.FiltroAuditoria;
import bo.phishshield.backend.auditoria.domain.model.RegistroAuditoria;
import bo.phishshield.backend.auditoria.domain.port.out.AuditoriaQueryPort;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.Pagina;
import bo.phishshield.backend.shared.domain.model.SolicitudPagina;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;

import java.util.List;

/**
 * Caso de uso: consultar el registro de accesos y acciones administrativas.
 * Solo el ADMIN: el registro mezcla datos de todas las empresas.
 */
public class ListarAuditoriaUseCase {

    private final AuditoriaQueryPort consultas;

    public ListarAuditoriaUseCase(AuditoriaQueryPort consultas) {
        this.consultas = consultas;
    }

    public Pagina<RegistroAuditoria> ejecutar(UsuarioActual quien, FiltroAuditoria filtro, SolicitudPagina pagina) {
        AlcanceDatos.soloPlataforma(quien);
        return consultas.buscar(filtro, pagina);
    }

    public List<String> acciones(UsuarioActual quien) {
        AlcanceDatos.soloPlataforma(quien);
        return consultas.acciones();
    }
}
