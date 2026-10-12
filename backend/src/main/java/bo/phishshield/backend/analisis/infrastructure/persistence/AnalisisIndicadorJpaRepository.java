package bo.phishshield.backend.analisis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AnalisisIndicadorJpaRepository
        extends JpaRepository<AnalisisIndicadorEntity, AnalisisIndicadorId> {

    /** Indicadores de varios analisis en una sola consulta (WHERE id_analisis IN ...). */
    List<AnalisisIndicadorEntity> findByIdAnalisisIn(Collection<Long> idsAnalisis);
}