package bo.phishshield.backend.usuario.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SesionJpaRepository extends JpaRepository<SesionEntity, Long> {

    Optional<SesionEntity> findByRefreshTokenHash(String refreshTokenHash);
}
