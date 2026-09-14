package bo.phishshield.backend.usuario.infrastructure.persistence;

import bo.phishshield.backend.usuario.domain.model.Usuario;
import bo.phishshield.backend.usuario.domain.port.out.UsuarioRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class UsuarioPersistenceAdapter implements UsuarioRepositoryPort {

    private final UsuarioJpaRepository jpaRepository;

    public UsuarioPersistenceAdapter(UsuarioJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<Usuario> listarTodos() {
        return jpaRepository.findAll()
                .stream()
                .map(this::aDominio)
                .toList();
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return jpaRepository.findById(id).map(this::aDominio);
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return jpaRepository.findByEmail(email).map(this::aDominio);
    }

    @Override
    public List<Usuario> listarPorEmpresa(Long idEmpresa) {
        return jpaRepository.findByIdEmpresa(idEmpresa)
                .stream()
                .map(this::aDominio)
                .toList();
    }

    private Usuario aDominio(UsuarioEntity entidad) {
        return new Usuario(
                entidad.getIdUsuario(),
                entidad.getUuidPublico(),
                entidad.getIdEmpresa(),
                entidad.getNombres(),
                entidad.getApellidos(),
                entidad.getEmail(),
                entidad.getRol(),
                entidad.getEstado(),
                entidad.getEmailVerificado(),
                entidad.getUltimoLogin(),
                entidad.getCreadoEn()
        );
    }
}