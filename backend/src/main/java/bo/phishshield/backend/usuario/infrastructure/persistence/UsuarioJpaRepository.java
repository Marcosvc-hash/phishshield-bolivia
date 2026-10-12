package bo.phishshield.backend.usuario.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, Long> {

    Optional<UsuarioEntity> findByEmail(String email);

    Optional<UsuarioEntity> findByUuidPublico(UUID uuidPublico);

    List<UsuarioEntity> findByIdEmpresa(Long idEmpresa);
}