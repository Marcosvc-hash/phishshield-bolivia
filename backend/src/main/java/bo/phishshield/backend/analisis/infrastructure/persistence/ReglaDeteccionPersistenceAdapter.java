package bo.phishshield.backend.analisis.infrastructure.persistence;

import bo.phishshield.backend.analisis.domain.port.out.ReglaDeteccionRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class ReglaDeteccionPersistenceAdapter implements ReglaDeteccionRepositoryPort {

    private final ReglaDeteccionJpaRepository jpaRepository;

    public ReglaDeteccionPersistenceAdapter(ReglaDeteccionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> pesosActivosPorCategoria(String categoria) {
        Map<String, BigDecimal> pesos = new HashMap<>();
        for (ReglaDeteccionEntity regla : jpaRepository.findByCategoriaAndActivaTrue(categoria)) {
            pesos.put(regla.getCodigo(), regla.getPeso());
        }
        return pesos;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> codigosRegistrados() {
        Set<String> codigos = new HashSet<>();
        for (ReglaDeteccionEntity regla : jpaRepository.findAll()) {
            codigos.add(regla.getCodigo());
        }
        return codigos;
    }
}