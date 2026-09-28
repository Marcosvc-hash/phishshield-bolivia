package bo.phishshield.backend.analisis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EntidadSuplantadaJpaRepository
        extends JpaRepository<EntidadSuplantadaEntity, Long> {

    List<EntidadSuplantadaEntity> findByDominioOficialIsNotNull();
}