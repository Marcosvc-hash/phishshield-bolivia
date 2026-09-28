package bo.phishshield.backend.analisis.infrastructure.persistence;

import bo.phishshield.backend.analisis.domain.model.DominioOficial;
import bo.phishshield.backend.analisis.domain.port.out.DominioOficialRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
public class DominioOficialPersistenceAdapter implements DominioOficialRepositoryPort {

    private final EntidadSuplantadaJpaRepository jpaRepository;

    public DominioOficialPersistenceAdapter(EntidadSuplantadaJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DominioOficial> listarActivos() {
        List<DominioOficial> dominios = new ArrayList<>();
        for (EntidadSuplantadaEntity entidad : jpaRepository.findByDominioOficialIsNotNull()) {
            if (entidad.getDominioOficial() != null && !entidad.getDominioOficial().isBlank()) {
                dominios.add(new DominioOficial(entidad.getNombre(), entidad.getDominioOficial()));
            }
        }
        return dominios;
    }
}