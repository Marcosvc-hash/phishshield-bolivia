package bo.phishshield.backend.empresa.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmpresaJpaRepository extends JpaRepository<EmpresaEntity, Long> {

    boolean existsByNit(String nit);

    Optional<EmpresaEntity> findByUuidPublico(UUID uuidPublico);
}
