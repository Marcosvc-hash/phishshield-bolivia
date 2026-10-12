package bo.phishshield.backend.analisis.infrastructure.persistence;

import bo.phishshield.backend.analisis.domain.port.out.ModeloIaRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class ModeloIaPersistenceAdapter implements ModeloIaRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(ModeloIaPersistenceAdapter.class);

    private final ModeloIaJpaRepository repository;

    public ModeloIaPersistenceAdapter(ModeloIaJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> buscarId(String nombre, String version) {
        if (nombre == null || version == null) {
            return Optional.empty();
        }
        Optional<Long> id = repository.findByNombreAndVersion(nombre, version).map(ModeloIaEntity::getIdModelo);
        if (id.isEmpty()) {
            log.warn("El modelo {} {} respondio pero no esta en modelos_ia: el analisis queda sin id_modelo. "
                    + "Revise la migracion V5.", nombre, version);
        }
        return id;
    }
}
