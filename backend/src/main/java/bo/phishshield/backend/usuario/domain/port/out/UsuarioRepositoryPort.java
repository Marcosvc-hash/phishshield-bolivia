package bo.phishshield.backend.usuario.domain.port.out;

import bo.phishshield.backend.usuario.domain.model.Usuario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepositoryPort {

    List<Usuario> listarTodos();

    Optional<Usuario> buscarPorId(Long id);

    Optional<Usuario> buscarPorUuid(UUID uuidPublico);

    Optional<Usuario> buscarPorEmail(String email);

    List<Usuario> listarPorEmpresa(Long idEmpresa);

    /** Inserta si el usuario no tiene id; si lo tiene, actualiza estado, intentos y ultimo login. */
    Usuario guardar(Usuario usuario);

    boolean existeEmail(String email);
}
