package bo.phishshield.backend.analisis.infrastructure.persistence;

import bo.phishshield.backend.analisis.domain.model.DominioListado;
import bo.phishshield.backend.analisis.domain.port.out.ListaNegraRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional(readOnly = true)
public class ListaNegraPersistenceAdapter implements ListaNegraRepositoryPort {

    private static final String LISTA_NEGRA = "negra";

    private final DominioListaJpaRepository jpaRepository;

    public ListaNegraPersistenceAdapter(DominioListaJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<DominioListado> listarActivos() {
        return jpaRepository.findByListaAndActivoTrue(LISTA_NEGRA)
                .stream()
                .map(e -> new DominioListado(e.getDominio(), e.getMotivo()))
                .toList();
    }
}
