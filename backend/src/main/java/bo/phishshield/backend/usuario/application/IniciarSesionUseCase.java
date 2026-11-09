package bo.phishshield.backend.usuario.application;

import bo.phishshield.backend.usuario.domain.model.CredencialesInvalidasException;
import bo.phishshield.backend.usuario.domain.model.CuentaNoHabilitadaException;
import bo.phishshield.backend.usuario.domain.model.EventoAcceso;
import bo.phishshield.backend.usuario.domain.model.SesionRefresco;
import bo.phishshield.backend.usuario.domain.model.TipoEventoAcceso;
import bo.phishshield.backend.usuario.domain.model.TokenRefresco;
import bo.phishshield.backend.usuario.domain.model.Usuario;
import bo.phishshield.backend.usuario.domain.port.out.PasswordHasherPort;
import bo.phishshield.backend.usuario.domain.port.out.RegistroAccesoPort;
import bo.phishshield.backend.usuario.domain.port.out.SesionRepositoryPort;
import bo.phishshield.backend.usuario.domain.port.out.TokenAccesoPort;
import bo.phishshield.backend.usuario.domain.port.out.UsuarioRepositoryPort;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Caso de uso: iniciar sesion con email y contrasena.
 *
 * Cada intento, exitoso o fallido, queda en el registro de accesos.
 * No hay una transaccion que envuelva todo a proposito: cuando el login
 * falla, el intento registrado debe quedar guardado aunque se lance la excepcion.
 */
public class IniciarSesionUseCase {

    private final UsuarioRepositoryPort usuarioRepository;
    private final PasswordHasherPort passwordHasher;
    private final TokenAccesoPort tokenAcceso;
    private final SesionRepositoryPort sesionRepository;
    private final RegistroAccesoPort registroAcceso;
    private final Clock reloj;
    private final Duration duracionRefresco;

    public IniciarSesionUseCase(UsuarioRepositoryPort usuarioRepository,
                                PasswordHasherPort passwordHasher,
                                TokenAccesoPort tokenAcceso,
                                SesionRepositoryPort sesionRepository,
                                RegistroAccesoPort registroAcceso,
                                Clock reloj,
                                Duration duracionRefresco) {
        this.usuarioRepository = usuarioRepository;
        this.passwordHasher = passwordHasher;
        this.tokenAcceso = tokenAcceso;
        this.sesionRepository = sesionRepository;
        this.registroAcceso = registroAcceso;
        this.reloj = reloj;
        this.duracionRefresco = duracionRefresco;
    }

    public SesionIniciada ejecutar(ComandoIniciarSesion comando) {
        Optional<Usuario> encontrado = usuarioRepository.buscarPorEmail(comando.email());

        if (encontrado.isEmpty()) {
            registrar(comando, TipoEventoAcceso.LOGIN_FALLIDO, null, "email_no_registrado");
            throw new CredencialesInvalidasException();
        }

        Usuario usuario = encontrado.get();

        if (!passwordHasher.coincide(comando.contrasena(), usuario.getPasswordHash())) {
            usuario.registrarLoginFallido();
            usuarioRepository.guardar(usuario);
            registrar(comando, TipoEventoAcceso.LOGIN_FALLIDO, usuario.getId(), "contrasena_incorrecta");
            throw new CredencialesInvalidasException();
        }

        if (!usuario.puedeIniciarSesion()) {
            registrar(comando, TipoEventoAcceso.LOGIN_FALLIDO, usuario.getId(),
                    "cuenta_" + usuario.getEstado().aValorBd());
            throw new CuentaNoHabilitadaException(usuario.getEstado());
        }

        OffsetDateTime ahora = OffsetDateTime.now(reloj);
        usuario.registrarLoginExitoso(ahora);
        usuarioRepository.guardar(usuario);

        String refresco = TokenRefresco.nuevo();
        OffsetDateTime refrescoExpira = ahora.plus(duracionRefresco);
        sesionRepository.guardar(SesionRefresco.nueva(
                usuario.getId(), TokenRefresco.hashDe(refresco), refrescoExpira, comando.userAgent()));

        registrar(comando, TipoEventoAcceso.LOGIN_EXITOSO, usuario.getId(), null);

        return new SesionIniciada(usuario, tokenAcceso.emitir(usuario), refresco, refrescoExpira);
    }

    private void registrar(ComandoIniciarSesion comando, TipoEventoAcceso tipo,
                           Long idUsuario, String motivo) {
        registroAcceso.registrar(new EventoAcceso(
                tipo, idUsuario, comando.email(), motivo, comando.ip(), comando.userAgent()));
    }
}
