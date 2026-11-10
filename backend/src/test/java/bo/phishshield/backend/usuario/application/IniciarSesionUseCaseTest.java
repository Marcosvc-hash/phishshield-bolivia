package bo.phishshield.backend.usuario.application;

import bo.phishshield.backend.shared.domain.model.Rol;
import bo.phishshield.backend.usuario.domain.model.CredencialesInvalidasException;
import bo.phishshield.backend.usuario.domain.model.CuentaNoHabilitadaException;
import bo.phishshield.backend.usuario.domain.model.EstadoUsuario;
import bo.phishshield.backend.usuario.domain.model.EventoAcceso;
import bo.phishshield.backend.usuario.domain.model.SesionRefresco;
import bo.phishshield.backend.usuario.domain.model.TipoEventoAcceso;
import bo.phishshield.backend.usuario.domain.model.TokenAcceso;
import bo.phishshield.backend.usuario.domain.model.TokenRefresco;
import bo.phishshield.backend.usuario.domain.model.Usuario;
import bo.phishshield.backend.usuario.domain.port.out.PasswordHasherPort;
import bo.phishshield.backend.usuario.domain.port.out.SesionRepositoryPort;
import bo.phishshield.backend.usuario.domain.port.out.UsuarioRepositoryPort;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class IniciarSesionUseCaseTest {

    // ---- Dobles de prueba en memoria ----

    static class UsuariosEnMemoria implements UsuarioRepositoryPort {
        final List<Usuario> usuarios = new ArrayList<>();

        @Override public List<Usuario> listarTodos() { return List.copyOf(usuarios); }
        @Override public Optional<Usuario> buscarPorId(Long id) {
            return usuarios.stream().filter(u -> id.equals(u.getId())).findFirst();
        }
        @Override public Optional<Usuario> buscarPorUuid(UUID uuid) {
            return usuarios.stream().filter(u -> uuid.equals(u.getUuidPublico())).findFirst();
        }
        @Override public Optional<Usuario> buscarPorEmail(String email) {
            return usuarios.stream().filter(u -> email.equals(u.getEmail())).findFirst();
        }
        @Override public List<Usuario> listarPorEmpresa(Long idEmpresa) { return List.of(); }
        @Override public Usuario guardar(Usuario usuario) { return usuario; }
        @Override public boolean existeEmail(String email) { return buscarPorEmail(email).isPresent(); }
    }

    /** "Hashea" anteponiendo un prefijo: basta para probar la logica sin BCrypt. */
    static class HasherFalso implements PasswordHasherPort {
        @Override public String hashear(String plana) { return "hash:" + plana; }
        @Override public boolean coincide(String plana, String hash) { return hash.equals("hash:" + plana); }
    }

    static class SesionesEnMemoria implements SesionRepositoryPort {
        final List<SesionRefresco> sesiones = new ArrayList<>();

        @Override public SesionRefresco guardar(SesionRefresco s) { sesiones.add(s); return s; }
        @Override public Optional<SesionRefresco> buscarPorHash(String hash) {
            return sesiones.stream().filter(s -> s.hashToken().equals(hash)).findFirst();
        }
        @Override public void revocar(Long idSesion) { }
    }

    private static final Instant AHORA = Instant.parse("2026-11-09T15:00:00Z");

    private final UsuariosEnMemoria usuarios = new UsuariosEnMemoria();
    private final SesionesEnMemoria sesiones = new SesionesEnMemoria();
    private final List<EventoAcceso> accesos = new ArrayList<>();

    private final IniciarSesionUseCase casoDeUso = new IniciarSesionUseCase(
            usuarios,
            new HasherFalso(),
            u -> new TokenAcceso("jwt-de-" + u.getEmail(), OffsetDateTime.ofInstant(AHORA, ZoneOffset.UTC)),
            sesiones,
            accesos::add,
            Clock.fixed(AHORA, ZoneOffset.UTC),
            Duration.ofDays(7));

    private Usuario agregarUsuario(String email, String contrasena, EstadoUsuario estado) {
        Usuario usuario = new Usuario(1L, UUID.randomUUID(), null, "Carlos", "Mamani",
                email, "hash:" + contrasena, Rol.USUARIO, estado, true, 0, null, null);
        usuarios.usuarios.add(usuario);
        return usuario;
    }

    private SesionIniciada login(String email, String contrasena) {
        return casoDeUso.ejecutar(new ComandoIniciarSesion(email, contrasena, "127.0.0.1", "JUnit"));
    }

    // ---- Pruebas ----

    @Test
    void login_correcto_entrega_tokens_y_registra_el_acceso() {
        Usuario usuario = agregarUsuario("carlos@andina.bo", "clave-segura-1", EstadoUsuario.ACTIVO);

        // El email se normaliza: mayusculas y espacios no impiden entrar
        SesionIniciada sesion = login("  Carlos@Andina.BO ", "clave-segura-1");

        assertEquals("jwt-de-carlos@andina.bo", sesion.tokenAcceso().valor());
        assertNotNull(sesion.tokenRefresco());
        assertEquals(OffsetDateTime.ofInstant(AHORA, ZoneOffset.UTC).plusDays(7), sesion.refrescoExpiraEn());

        // En la base queda el hash del token de refresco, nunca el valor
        assertEquals(1, sesiones.sesiones.size());
        assertEquals(TokenRefresco.hashDe(sesion.tokenRefresco()), sesiones.sesiones.get(0).hashToken());

        assertEquals(TipoEventoAcceso.LOGIN_EXITOSO, accesos.get(0).tipo());
        assertNotNull(usuario.getUltimoLogin());
    }

    @Test
    void contrasena_incorrecta_lanza_credenciales_invalidas_y_registra_el_intento() {
        Usuario usuario = agregarUsuario("carlos@andina.bo", "clave-segura-1", EstadoUsuario.ACTIVO);

        assertThrows(CredencialesInvalidasException.class,
                () -> login("carlos@andina.bo", "otra-clave"));

        assertEquals(1, usuario.getIntentosFallidos());
        assertTrue(sesiones.sesiones.isEmpty());
        assertEquals(TipoEventoAcceso.LOGIN_FALLIDO, accesos.get(0).tipo());
        assertEquals("contrasena_incorrecta", accesos.get(0).motivo());
    }

    @Test
    void email_no_registrado_da_el_mismo_error_que_una_contrasena_incorrecta() {
        CredencialesInvalidasException ex = assertThrows(CredencialesInvalidasException.class,
                () -> login("nadie@andina.bo", "lo-que-sea"));

        assertEquals("Email o contrasena incorrectos", ex.getMessage());
        assertNull(accesos.get(0).idUsuario());
        assertEquals("email_no_registrado", accesos.get(0).motivo());
    }

    @Test
    void una_cuenta_inactiva_no_puede_entrar_aunque_la_contrasena_sea_correcta() {
        agregarUsuario("carlos@andina.bo", "clave-segura-1", EstadoUsuario.INACTIVO);

        assertThrows(CuentaNoHabilitadaException.class,
                () -> login("carlos@andina.bo", "clave-segura-1"));

        assertTrue(sesiones.sesiones.isEmpty());
        assertEquals("cuenta_inactivo", accesos.get(0).motivo());
    }
}
