package bo.phishshield.backend.empresa.infrastructure.persistence;

import bo.phishshield.backend.empresa.domain.model.Empresa;
import bo.phishshield.backend.empresa.domain.port.out.EmpresaRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class EmpresaPersistenceAdapter implements EmpresaRepositoryPort {

    private final EmpresaJpaRepository jpaRepository;

    public EmpresaPersistenceAdapter(EmpresaJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<Empresa> listarTodas() {
        return jpaRepository.findAll()
                .stream()
                .map(this::aDominio)
                .toList();
    }

    @Override
    public Optional<Empresa> buscarPorId(Long id) {
        return jpaRepository.findById(id).map(this::aDominio);
    }

    private Empresa aDominio(EmpresaEntity entidad) {
        return new Empresa(
                entidad.getIdEmpresa(),
                entidad.getUuidPublico(),
                entidad.getNombre(),
                entidad.getNit(),
                entidad.getSector(),
                entidad.getCiudad(),
                entidad.getPlan(),
                entidad.getActiva(),
                entidad.getCreadoEn()
        );
    }
}