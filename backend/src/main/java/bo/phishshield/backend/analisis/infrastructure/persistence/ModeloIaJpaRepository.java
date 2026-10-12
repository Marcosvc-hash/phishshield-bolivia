package bo.phishshield.backend.analisis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ModeloIaJpaRepository extends JpaRepository<ModeloIaEntity, Long> {

    Optional<ModeloIaEntity> findByNombreAndVersion(String nombre, String version);
}
