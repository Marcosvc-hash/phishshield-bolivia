package bo.phishshield.backend.analisis.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnalisisJpaRepository extends JpaRepository<AnalisisEntity, Long> {

    /** Puede haber varias filas con el mismo hash (una por recalculo): se toma la mas reciente. */
    Optional<AnalisisEntity> findFirstByContenidoHashOrderByFechaAnalisisDesc(String contenidoHash);

    Optional<AnalisisEntity> findByUuidPublico(UUID uuidPublico);

    List<AnalisisEntity> findAllByOrderByFechaAnalisisDesc(Pageable pageable);
}