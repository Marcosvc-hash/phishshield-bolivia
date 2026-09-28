package bo.phishshield.backend.analisis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReglaDeteccionJpaRepository extends JpaRepository<ReglaDeteccionEntity, Long> {

    List<ReglaDeteccionEntity> findByCategoriaAndActivaTrue(String categoria);

    List<ReglaDeteccionEntity> findByActivaTrue();
}