package bo.phishshield.backend.usuario.application;

import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import bo.phishshield.backend.usuario.domain.model.Usuario;
import bo.phishshield.backend.usuario.domain.port.out.UsuarioRepositoryPort;

import java.util.Comparator;
import java.util.List;

/**
 * Caso de uso: listar usuarios para administrarlos.
 * El ADMIN ve a todos; el ADMIN_EMPRESA, solo a los de su empresa.
 */
public class ListarUsuariosUseCase {

    private final UsuarioRepositoryPort usuarioRepository;

    public ListarUsuariosUseCase(UsuarioRepositoryPort usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public List<Usuario> ejecutar(UsuarioActual quien) {
        AlcanceDatos alcance = AlcanceDatos.administrativo(quien);

        List<Usuario> usuarios = alcance.esGlobal()
                ? usuarioRepository.listarTodos()
                : usuarioRepository.listarPorEmpresa(alcance.idEmpresa());

        return usuarios.stream()
                .sorted(Comparator.comparing(Usuario::nombreCompleto, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
