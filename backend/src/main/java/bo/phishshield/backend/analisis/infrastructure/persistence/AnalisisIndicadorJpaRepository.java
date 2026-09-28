package bo.phishshield.backend.analisis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalisisIndicadorJpaRepository
        extends JpaRepository<AnalisisIndicadorEntity, AnalisisIndicadorId> {

    List<AnalisisIndicadorEntity> findByIdAnalisis(Long idAnalisis);
}