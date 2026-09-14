package bo.phishshield.backend.empresa.infrastructure.persistence;

import bo.phishshield.backend.empresa.domain.model.Empresa;
import bo.phishshield.backend.empresa.domain.port.out.EmpresaRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.OffsetDateTime;

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

    @Override
    public Empresa guardar(Empresa empresa) {
        EmpresaEntity entidad = aEntidad(empresa);
        EmpresaEntity guardada = jpaRepository.save(entidad);
        return aDominio(guardada);
    }

    @Override
    public boolean existeNit(String nit) {
        return nit != null && jpaRepository.existsByNit(nit);
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

    private EmpresaEntity aEntidad(Empresa empresa) {
        OffsetDateTime ahora = OffsetDateTime.now();

        EmpresaEntity entidad = new EmpresaEntity();
        entidad.setUuidPublico(
                empresa.getUuidPublico() != null
                        ? empresa.getUuidPublico()
                        : UUID.randomUUID());
        entidad.setNombre(empresa.getNombre());
        entidad.setNit(empresa.getNit());
        entidad.setSector(empresa.getSector());
        entidad.setCiudad(empresa.getCiudad());
        entidad.setPlan(empresa.getPlan());
        entidad.setActiva(empresa.isActiva());
        entidad.setCreadoEn(
                empresa.getCreadoEn() != null
                        ? empresa.getCreadoEn()
                        : ahora);
        entidad.setActualizadoEn(ahora);
        return entidad;
    }
}
