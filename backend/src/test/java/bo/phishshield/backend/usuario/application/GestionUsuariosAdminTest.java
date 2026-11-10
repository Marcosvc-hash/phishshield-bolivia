package bo.phishshield.backend.usuario.application;

import bo.phishshield.backend.empresa.domain.model.Empresa;
import bo.phishshield.backend.empresa.domain.port.out.EmpresaRepositoryPort;
import bo.phishshield.backend.shared.dobles.AuditoriaEnMemoria;
import bo.phishshield.backend.shared.domain.model.AccesoDenegadoException;
import bo.phishshield.backend.shared.domain.model.AccionAuditoria;
import bo.phishshield.backend.shared.domain.model.Rol;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import bo.phishshield.backend.usuario.domain.model.EmailYaRegistradoException;
import bo.phishshield.backend.usuario.domain.model.EstadoUsuario;
import bo.phishshield.backend.usuario.domain.model.Usuario;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Alta de usuarios desde el panel y cambio de rol: permisos por rol y por empresa. */
class GestionUsuariosAdminTest {

    /** Como el doble de IniciarSesionUseCaseTest, pero guardar() da id a los nuevos. */
    static class UsuariosConAlta extends IniciarSesionUseCaseTest.UsuariosEnMemoria {
        @Override
        public Usuario guardar(Usuario u) {
            if (u.getId() != null) {
                return u;
            }
            Usuario conId = new Usuario((long) usuarios.size() + 100, UUID.randomUUID(), u.getIdEmpresa(),
                    u.getNombres(), u.getApellidos(), u.getEmail(), u.getPasswordHash(), u.getRol(),
                    u.getEstado(), u.isEmailVerificado(), 0, null, null);
            usuarios.add(conId);
            return conId;
        }
    }

    static class EmpresasFijas implements EmpresaRepositoryPort {
        final List<Empresa> empresas = List.of(
                new Empresa(1L, UUID.randomUUID(), "Importadora Andina", null, null, null, "basico", true, null),
                new Empresa(2L, UUID.randomUUID(), "Clinica San Rafael", null, null, null, "basico", true, null),
                new Empresa(3L, UUID.randomUUID(), "Empresa Cerrada", null, null, null, "basico", false, null));

        @Override public List<Empresa> listarTodas() { return empresas; }
        @Override public Optional<Empresa> buscarPorId(Long id) {
            return empresas.stream().filter(e -> e.getId().equals(id)).findFirst();
        }
        @Override public Optional<Empresa> buscarPorUuid(UUID uuid) {
            return empresas.stream().filter(e -> e.getUuidPublico().equals(uuid)).findFirst();
        }
        @Override public Empresa guardar(Empresa empresa) { return empresa; }
        @Override public boolean existeNit(String nit) { return false; }
        @Override public boolean existeNitEnOtra(String nit, Long idEmpresa) { return false; }

        UUID uuid(long id) { return buscarPorId(id).orElseThrow().getUuidPublico(); }
    }

    private static final String IP = "190.129.10.25";
    private static final String CLAVE = "ClaveInicial123";

    private final UsuariosConAlta usuarios = new UsuariosConAlta();
    private final EmpresasFijas empresas = new EmpresasFijas();
    private final AuditoriaEnMemoria auditoria = new AuditoriaEnMemoria();
    private final CrearUsuarioAdminUseCase crear = new CrearUsuarioAdminUseCase(
            usuarios, new IniciarSesionUseCaseTest.HasherFalso(), empresas, auditoria);
    private final CambiarRolUsuarioUseCase cambiarRol = new CambiarRolUsuarioUseCase(usuarios, auditoria);

    private static final UsuarioActual ADMIN = new UsuarioActual(1L, UUID.randomUUID(), "admin@x.bo", null, Rol.ADMIN);
    private static final UsuarioActual MARIA = new UsuarioActual(2L, UUID.randomUUID(), "maria@andina.bo", 1L, Rol.ADMIN_EMPRESA);

    private ComandoCrearUsuario alta(String email, Rol rol, UUID empresa) {
        return new ComandoCrearUsuario("Nuevo", null, email, CLAVE, rol, empresa, IP);
    }

    private Usuario existente(long id, Rol rol, Long idEmpresa) {
        Usuario u = new Usuario(id, UUID.randomUUID(), idEmpresa, "Usuario " + id, null,
                "u" + id + "@x.bo", "hash", rol, EstadoUsuario.ACTIVO, true, 0, null, null);
        usuarios.usuarios.add(u);
        return u;
    }

    // ---- Alta ----

    @Test
    void el_admin_crea_un_admin_de_empresa_activo_con_el_email_en_minusculas() {
        Usuario creado = crear.ejecutar(ADMIN, alta("  Lucia@SanRafael.BO ", Rol.ADMIN_EMPRESA, empresas.uuid(2)));

        assertEquals("lucia@sanrafael.bo", creado.getEmail());
        assertEquals(Rol.ADMIN_EMPRESA, creado.getRol());
        assertEquals(2L, creado.getIdEmpresa());
        assertEquals(EstadoUsuario.ACTIVO, creado.getEstado());
        assertEquals("hash:" + CLAVE, creado.getPasswordHash(), "La contrasena se guarda hasheada");
        assertEquals(List.of(AccionAuditoria.USUARIO_CREADO), auditoria.acciones());
        assertEquals("Clinica San Rafael", auditoria.ultimo().detalle().get("empresa"));
    }

    @Test
    void el_admin_de_empresa_crea_usuarios_en_su_empresa_aunque_no_la_indique() {
        Usuario creado = crear.ejecutar(MARIA, alta("pedro@andina.bo", Rol.USUARIO, null));

        assertEquals(1L, creado.getIdEmpresa());
    }

    @Test
    void el_admin_de_empresa_nunca_crea_un_admin() {
        assertThrows(AccesoDenegadoException.class, () -> crear.ejecutar(MARIA, alta("x@andina.bo", Rol.ADMIN, null)));
        assertTrue(usuarios.usuarios.isEmpty());
    }

    @Test
    void el_admin_de_empresa_no_crea_usuarios_en_otra_empresa() {
        assertThrows(AccesoDenegadoException.class,
                () -> crear.ejecutar(MARIA, alta("x@sanrafael.bo", Rol.USUARIO, empresas.uuid(2))));
    }

    @Test
    void el_permiso_se_revisa_antes_que_el_email_repetido() {
        existente(50, Rol.USUARIO, 2L);   // u50@x.bo

        // Si se revisara el email primero, MARIA sabria que u50@x.bo existe
        assertThrows(AccesoDenegadoException.class, () -> crear.ejecutar(MARIA, alta("u50@x.bo", Rol.ADMIN, null)));
        assertThrows(EmailYaRegistradoException.class, () -> crear.ejecutar(ADMIN, alta("u50@x.bo", Rol.USUARIO, null)));
    }

    @Test
    void un_admin_de_empresa_necesita_empresa_y_un_admin_no_lleva() {
        assertThrows(IllegalArgumentException.class, () -> crear.ejecutar(ADMIN, alta("a@x.bo", Rol.ADMIN_EMPRESA, null)));
        assertThrows(IllegalArgumentException.class, () -> crear.ejecutar(ADMIN, alta("b@x.bo", Rol.ADMIN, empresas.uuid(1))));
        assertNull(crear.ejecutar(ADMIN, alta("c@x.bo", Rol.ADMIN, null)).getIdEmpresa());
    }

    @Test
    void una_empresa_desactivada_no_recibe_usuarios_nuevos() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> crear.ejecutar(ADMIN, alta("x@cerrada.bo", Rol.USUARIO, empresas.uuid(3))));
        assertTrue(error.getMessage().contains("desactivada"));
    }

    @Test
    void una_contrasena_corta_es_rechazada() {
        assertThrows(IllegalArgumentException.class, () -> crear.ejecutar(ADMIN,
                new ComandoCrearUsuario("X", null, "x@x.bo", "corta", Rol.USUARIO, null, IP)));
    }

    @Test
    void un_usuario_comun_no_crea_cuentas() {
        UsuarioActual carlos = new UsuarioActual(9L, UUID.randomUUID(), "c@andina.bo", 1L, Rol.USUARIO);
        assertThrows(AccesoDenegadoException.class, () -> crear.ejecutar(carlos, alta("x@x.bo", Rol.USUARIO, null)));
    }

    // ---- Cambio de rol ----

    @Test
    void el_admin_asciende_a_admin_de_empresa_y_queda_en_auditoria() {
        Usuario carlos = existente(10, Rol.USUARIO, 1L);

        cambiarRol.ejecutar(ADMIN, carlos.getUuidPublico(), Rol.ADMIN_EMPRESA, IP);

        assertEquals(Rol.ADMIN_EMPRESA, carlos.getRol());
        assertEquals(List.of(AccionAuditoria.USUARIO_ROL_CAMBIADO), auditoria.acciones());
        assertEquals("USUARIO", auditoria.ultimo().detalle().get("rol_anterior"));
    }

    @Test
    void el_admin_de_empresa_cambia_roles_en_su_empresa_pero_nunca_a_admin() {
        Usuario carlos = existente(10, Rol.USUARIO, 1L);

        cambiarRol.ejecutar(MARIA, carlos.getUuidPublico(), Rol.ADMIN_EMPRESA, IP);
        assertEquals(Rol.ADMIN_EMPRESA, carlos.getRol());

        assertThrows(AccesoDenegadoException.class,
                () -> cambiarRol.ejecutar(MARIA, carlos.getUuidPublico(), Rol.ADMIN, IP));
        assertEquals(Rol.ADMIN_EMPRESA, carlos.getRol());
    }

    @Test
    void el_admin_de_empresa_no_toca_usuarios_de_otra_empresa_ni_admins() {
        Usuario lucia = existente(11, Rol.USUARIO, 2L);
        Usuario admin = existente(12, Rol.ADMIN, 1L);

        assertThrows(AccesoDenegadoException.class,
                () -> cambiarRol.ejecutar(MARIA, lucia.getUuidPublico(), Rol.ADMIN_EMPRESA, IP));
        assertThrows(AccesoDenegadoException.class,
                () -> cambiarRol.ejecutar(MARIA, admin.getUuidPublico(), Rol.USUARIO, IP));
    }

    @Test
    void nadie_cambia_su_propio_rol() {
        Usuario yo = existente(1, Rol.ADMIN, null);

        assertThrows(AccesoDenegadoException.class,
                () -> cambiarRol.ejecutar(ADMIN, yo.getUuidPublico(), Rol.USUARIO, IP));
    }

    @Test
    void sin_empresa_no_se_puede_ser_admin_de_empresa() {
        Usuario suelto = existente(13, Rol.USUARIO, null);

        assertThrows(IllegalArgumentException.class,
                () -> cambiarRol.ejecutar(ADMIN, suelto.getUuidPublico(), Rol.ADMIN_EMPRESA, IP));
    }

    @Test
    void pedir_el_mismo_rol_no_cambia_nada_ni_se_audita() {
        Usuario carlos = existente(10, Rol.USUARIO, 1L);

        cambiarRol.ejecutar(ADMIN, carlos.getUuidPublico(), Rol.USUARIO, IP);

        assertTrue(auditoria.eventos.isEmpty());
    }

    @Test
    void los_nombres_de_rol_de_la_api_se_traducen() {
        assertEquals(Rol.ADMIN_EMPRESA, Rol.desdeNombre(" admin_empresa "));
        assertThrows(IllegalArgumentException.class, () -> Rol.desdeNombre("analista"));
    }
}
