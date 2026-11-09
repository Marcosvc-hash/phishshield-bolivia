package bo.phishshield.backend.usuario.infrastructure.persistence;

import bo.phishshield.backend.shared.domain.model.Rol;
import bo.phishshield.backend.usuario.domain.model.EstadoUsuario;
import bo.phishshield.backend.usuario.domain.model.Usuario;
import bo.phishshield.backend.usuario.domain.port.out.UsuarioRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    public Optional<Usuario> buscarPorUuid(UUID uuidPublico) {
        return jpaRepository.findByUuidPublico(uuidPublico).map(this::aDominio);
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

    /**
     * Al actualizar solo se copian los campos que el dominio puede cambiar
     * (estado, intentos, ultimo login); los demas quedan como estan en la base.
     */
    @Override
    @Transactional
    public Usuario guardar(Usuario usuario) {
        OffsetDateTime ahora = OffsetDateTime.now();
        UsuarioEntity entidad;

        if (usuario.getId() == null) {
            entidad = new UsuarioEntity();
            entidad.setUuidPublico(UUID.randomUUID());
            entidad.setIdEmpresa(usuario.getIdEmpresa());
            entidad.setNombres(usuario.getNombres());
            entidad.setApellidos(usuario.getApellidos());
            entidad.setEmail(usuario.getEmail());
            entidad.setPasswordHash(usuario.getPasswordHash());
            entidad.setRol(usuario.getRol().aValorBd());
            entidad.setEmailVerificado(usuario.isEmailVerificado());
            entidad.setCreadoEn(ahora);
        } else {
            entidad = jpaRepository.findById(usuario.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "No existe el usuario con id " + usuario.getId()));
        }

        entidad.setEstado(usuario.getEstado().aValorBd());
        entidad.setIntentosFallidos((short) usuario.getIntentosFallidos());
        entidad.setUltimoLogin(usuario.getUltimoLogin());
        entidad.setActualizadoEn(ahora);

        return aDominio(jpaRepository.save(entidad));
    }

    @Override
    public boolean existeEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    private Usuario aDominio(UsuarioEntity entidad) {
        return new Usuario(
                entidad.getIdUsuario(),
                entidad.getUuidPublico(),
                entidad.getIdEmpresa(),
                entidad.getNombres(),
                entidad.getApellidos(),
                entidad.getEmail(),
                entidad.getPasswordHash(),
                Rol.desdeValorBd(entidad.getRol()),
                EstadoUsuario.desdeValorBd(entidad.getEstado()),
                entidad.getEmailVerificado(),
                entidad.getIntentosFallidos(),
                entidad.getUltimoLogin(),
                entidad.getCreadoEn()
        );
    }
}
