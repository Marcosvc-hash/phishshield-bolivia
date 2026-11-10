package bo.phishshield.backend.auditoria.application;

import bo.phishshield.backend.auditoria.domain.model.FiltroAuditoria;
import bo.phishshield.backend.auditoria.domain.model.RegistroAuditoria;
import bo.phishshield.backend.auditoria.domain.port.out.AuditoriaQueryPort;
import bo.phishshield.backend.shared.domain.model.AccesoDenegadoException;
import bo.phishshield.backend.shared.domain.model.Pagina;
import bo.phishshield.backend.shared.domain.model.Rol;
import bo.phishshield.backend.shared.domain.model.SolicitudPagina;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ListarAuditoriaUseCaseTest {

    static class AuditoriaConsultasEnMemoria implements AuditoriaQueryPort {
        FiltroAuditoria ultimoFiltro;

        @Override
        public Pagina<RegistroAuditoria> buscar(FiltroAuditoria filtro, SolicitudPagina pagina) {
            ultimoFiltro = filtro;
            return Pagina.de(List.of(), pagina, 0);
        }

        @Override
        public List<String> acciones() {
            return List.of("LOGIN_EXITOSO", "LOGIN_FALLIDO");
        }
    }

    private final AuditoriaConsultasEnMemoria consultas = new AuditoriaConsultasEnMemoria();
    private final ListarAuditoriaUseCase casoDeUso = new ListarAuditoriaUseCase(consultas);

    private static UsuarioActual con(Rol rol, Long idEmpresa) {
        return new UsuarioActual(1L, UUID.randomUUID(), "x@x.bo", idEmpresa, rol);
    }

    @Test
    void el_admin_consulta_el_registro_con_los_filtros_normalizados() {
        casoDeUso.ejecutar(con(Rol.ADMIN, null),
                new FiltroAuditoria(" login_fallido ", "  Carlos@Andina.BO ", null),
                new SolicitudPagina(0, 20));

        assertEquals("LOGIN_FALLIDO", consultas.ultimoFiltro.accion());
        assertEquals("carlos@andina.bo", consultas.ultimoFiltro.usuario());
        assertNull(consultas.ultimoFiltro.fechas().inicio());
    }

    @Test
    void el_admin_de_empresa_no_ve_el_registro_ni_las_acciones() {
        UsuarioActual maria = con(Rol.ADMIN_EMPRESA, 5L);

        assertThrows(AccesoDenegadoException.class, () -> casoDeUso.ejecutar(
                maria, new FiltroAuditoria(null, null, null), new SolicitudPagina(0, 20)));
        assertThrows(AccesoDenegadoException.class, () -> casoDeUso.acciones(maria));
        assertNull(consultas.ultimoFiltro, "No debe llegar a consultar la base");
    }

    @Test
    void un_usuario_comun_tampoco() {
        assertThrows(AccesoDenegadoException.class, () -> casoDeUso.acciones(con(Rol.USUARIO, null)));
    }
}
