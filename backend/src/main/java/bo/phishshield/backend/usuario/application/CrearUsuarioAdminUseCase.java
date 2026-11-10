package bo.phishshield.backend.usuario.application;

import bo.phishshield.backend.empresa.domain.model.Empresa;
import bo.phishshield.backend.empresa.domain.port.out.EmpresaRepositoryPort;
import bo.phishshield.backend.shared.domain.model.AccionAuditoria;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.EventoAuditoria;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import bo.phishshield.backend.shared.domain.port.out.AuditoriaPort;
import bo.phishshield.backend.usuario.domain.model.EmailYaRegistradoException;
import bo.phishshield.backend.usuario.domain.model.PoliticaContrasena;
import bo.phishshield.backend.usuario.domain.model.Usuario;
import bo.phishshield.backend.usuario.domain.port.out.PasswordHasherPort;
import bo.phishshield.backend.usuario.domain.port.out.UsuarioRepositoryPort;
import bo.phishshield.backend.usuario.domain.service.PoliticaGestionUsuarios;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Caso de uso: un administrador crea una cuenta desde el panel.
 *
 * Orden de los pasos: primero los permisos (rol y empresa), despues la
 * empresa activa, la contrasena y el email repetido. Asi un ADMIN_EMPRESA
 * no puede usar este endpoint para averiguar que emails existen en otras
 * empresas: lo rechaza antes de mirar el email.
 */
public class CrearUsuarioAdminUseCase {

    private final UsuarioRepositoryPort usuarioRepository;
    private final PasswordHasherPort passwordHasher;
    private final EmpresaRepositoryPort empresaRepository;
    private final AuditoriaPort auditoria;

    public CrearUsuarioAdminUseCase(UsuarioRepositoryPort usuarioRepository,
                                    PasswordHasherPort passwordHasher,
                                    EmpresaRepositoryPort empresaRepository,
                                    AuditoriaPort auditoria) {
        this.usuarioRepository = usuarioRepository;
        this.passwordHasher = passwordHasher;
        this.empresaRepository = empresaRepository;
        this.auditoria = auditoria;
    }

    public Usuario ejecutar(UsuarioActual quien, ComandoCrearUsuario comando) {
        // Un USUARIO recibe 403 antes de que se busque nada en la base
        AlcanceDatos.administrativo(quien);

        Long idPedida = comando.empresa() == null ? null : empresaRepository.buscarPorUuid(comando.empresa())
                .map(Empresa::getId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la empresa " + comando.empresa()));

        Long idEmpresa = PoliticaGestionUsuarios.empresaParaAlta(quien, comando.rol(), idPedida);

        Empresa empresa = null;
        if (idEmpresa != null) {
            empresa = empresaRepository.buscarPorId(idEmpresa)
                    .orElseThrow(() -> new IllegalArgumentException("No existe la empresa del usuario"));
            PoliticaGestionUsuarios.validarEmpresaActiva(empresa.isActiva(), empresa.getNombre());
        }

        PoliticaContrasena.validar(comando.contrasena());
        if (comando.email() != null && usuarioRepository.existeEmail(comando.email())) {
            throw new EmailYaRegistradoException(comando.email());
        }

        Usuario guardado = usuarioRepository.guardar(Usuario.creadoPorAdmin(
                comando.nombres(),
                comando.apellidos(),
                comando.email(),
                passwordHasher.hashear(comando.contrasena()),
                comando.rol(),
                idEmpresa));

        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("email", guardado.getEmail());
        detalle.put("rol", guardado.getRol().name());
        detalle.put("empresa", empresa == null ? null : empresa.getNombre());
        auditoria.registrar(EventoAuditoria.de(AccionAuditoria.USUARIO_CREADO,
                quien, "usuarios", guardado.getId(), detalle, comando.ip()));

        return guardado;
    }
}
