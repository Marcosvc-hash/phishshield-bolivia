package bo.phishshield.backend.analisis.infrastructure.persistence;

import bo.phishshield.backend.analisis.domain.model.DominioListado;
import bo.phishshield.backend.analisis.domain.port.out.ListaBlancaRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional(readOnly = true)
public class ListaBlancaPersistenceAdapter implements ListaBlancaRepositoryPort {

    private static final String LISTA_BLANCA = "blanca";

    private final DominioListaJpaRepository jpaRepository;

    public ListaBlancaPersistenceAdapter(DominioListaJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<DominioListado> listarActivos() {
        return jpaRepository.findByListaAndActivoTrue(LISTA_BLANCA)
                .stream()
                .map(e -> new DominioListado(e.getDominio(), e.getMotivo()))
                .toList();
    }
}
