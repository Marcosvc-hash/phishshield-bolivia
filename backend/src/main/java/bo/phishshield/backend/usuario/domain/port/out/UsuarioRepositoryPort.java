package bo.phishshield.backend.usuario.domain.port.out;

import bo.phishshield.backend.usuario.domain.model.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepositoryPort {

    List<Usuario> listarTodos();

    Optional<Usuario> buscarPorId(Long id);

    Optional<Usuario> buscarPorEmail(String email);

    List<Usuario> listarPorEmpresa(Long idEmpresa);
}