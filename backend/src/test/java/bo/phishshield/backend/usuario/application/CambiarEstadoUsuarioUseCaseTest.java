package bo.phishshield.backend.usuario.application;

import bo.phishshield.backend.shared.dobles.AuditoriaEnMemoria;
import bo.phishshield.backend.shared.domain.model.AccesoDenegadoException;
import bo.phishshield.backend.shared.domain.model.AccionAuditoria;
import bo.phishshield.backend.shared.domain.model.Rol;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import bo.phishshield.backend.usuario.domain.model.EstadoUsuario;
import bo.phishshield.backend.usuario.domain.model.Usuario;
import bo.phishshield.backend.usuario.domain.model.UsuarioNoEncontradoException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CambiarEstadoUsuarioUseCaseTest {

    private static final long EMPRESA_ANDINA = 1L;
    private static final long OTRA_EMPRESA = 2L;
    private static final String IP = "190.129.10.25";

    private final IniciarSesionUseCaseTest.UsuariosEnMemoria usuarios =
            new IniciarSesionUseCaseTest.UsuariosEnMemoria();
    private final AuditoriaEnMemoria auditoria = new AuditoriaEnMemoria();
    private final CambiarEstadoUsuarioUseCase casoDeUso = new CambiarEstadoUsuarioUseCase(usuarios, auditoria);

    private Usuario agregar(long id, Rol rol, Long idEmpresa) {
        Usuario u = new Usuario(id, UUID.randomUUID(), idEmpresa, "Usuario " + id, null,
                "u" + id + "@andina.bo", "hash", rol, EstadoUsuario.ACTIVO, true, 0, null, null);
        usuarios.usuarios.add(u);
        return u;
    }

    private static UsuarioActual comoActual(Usuario u) {
        return new UsuarioActual(u.getId(), u.getUuidPublico(), u.getEmail(), u.getIdEmpresa(), u.getRol());
    }

    @Test
    void el_admin_desactiva_y_vuelve_a_activar_a_un_usuario() {
        Usuario admin = agregar(1, Rol.ADMIN, null);
        Usuario carlos = agregar(2, Rol.USUARIO, EMPRESA_ANDINA);

        casoDeUso.ejecutar(comoActual(admin), carlos.getUuidPublico(), false, IP);
        assertEquals(EstadoUsuario.INACTIVO, carlos.getEstado());
        assertFalse(carlos.puedeIniciarSesion());

        casoDeUso.ejecutar(comoActual(admin), carlos.getUuidPublico(), true, IP);
        assertEquals(EstadoUsuario.ACTIVO, carlos.getEstado());
    }

    @Test
    void cada_cambio_de_estado_queda_en_auditoria_con_quien_y_la_ip() {
        Usuario admin = agregar(1, Rol.ADMIN, null);
        Usuario carlos = agregar(2, Rol.USUARIO, EMPRESA_ANDINA);

        casoDeUso.ejecutar(comoActual(admin), carlos.getUuidPublico(), false, IP);
        casoDeUso.ejecutar(comoActual(admin), carlos.getUuidPublico(), true, IP);

        assertEquals(java.util.List.of(AccionAuditoria.USUARIO_DESACTIVADO, AccionAuditoria.USUARIO_ACTIVADO),
                auditoria.acciones());
        assertEquals(1L, auditoria.ultimo().idUsuario());
        assertEquals(2L, auditoria.ultimo().idEntidadAfectada());
        assertEquals(IP, auditoria.ultimo().ip());
        assertEquals("inactivo", auditoria.ultimo().detalle().get("estado_anterior"));
    }

    @Test
    void nadie_puede_desactivar_su_propia_cuenta() {
        Usuario admin = agregar(1, Rol.ADMIN, null);

        assertThrows(AccesoDenegadoException.class,
                () -> casoDeUso.ejecutar(comoActual(admin), admin.getUuidPublico(), false, IP));
        assertEquals(EstadoUsuario.ACTIVO, admin.getEstado());
        assertTrue(auditoria.eventos.isEmpty(), "Un intento rechazado no se registra como cambio");
    }

    @Test
    void el_admin_de_empresa_solo_gestiona_usuarios_de_su_empresa() {
        Usuario maria = agregar(1, Rol.ADMIN_EMPRESA, EMPRESA_ANDINA);
        Usuario carlos = agregar(2, Rol.USUARIO, EMPRESA_ANDINA);
        Usuario lucia = agregar(3, Rol.USUARIO, OTRA_EMPRESA);

        casoDeUso.ejecutar(comoActual(maria), carlos.getUuidPublico(), false, IP);
        assertEquals(EstadoUsuario.INACTIVO, carlos.getEstado());

        assertThrows(AccesoDenegadoException.class,
                () -> casoDeUso.ejecutar(comoActual(maria), lucia.getUuidPublico(), false, IP));
        assertEquals(EstadoUsuario.ACTIVO, lucia.getEstado());
    }

    @Test
    void el_admin_de_empresa_no_puede_tocar_a_un_admin_de_la_plataforma() {
        Usuario maria = agregar(1, Rol.ADMIN_EMPRESA, EMPRESA_ANDINA);
        Usuario admin = agregar(2, Rol.ADMIN, EMPRESA_ANDINA);

        assertThrows(AccesoDenegadoException.class,
                () -> casoDeUso.ejecutar(comoActual(maria), admin.getUuidPublico(), false, IP));
    }

    @Test
    void un_usuario_comun_no_gestiona_a_nadie() {
        Usuario carlos = agregar(1, Rol.USUARIO, EMPRESA_ANDINA);
        Usuario otro = agregar(2, Rol.USUARIO, EMPRESA_ANDINA);

        assertThrows(AccesoDenegadoException.class,
                () -> casoDeUso.ejecutar(comoActual(carlos), otro.getUuidPublico(), false, IP));
    }

    @Test
    void un_id_inexistente_da_usuario_no_encontrado() {
        Usuario admin = agregar(1, Rol.ADMIN, null);

        assertThrows(UsuarioNoEncontradoException.class,
                () -> casoDeUso.ejecutar(comoActual(admin), UUID.randomUUID(), false, IP));
    }
}
