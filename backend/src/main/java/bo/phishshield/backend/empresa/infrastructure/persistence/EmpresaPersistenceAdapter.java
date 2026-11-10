package bo.phishshield.backend.empresa.infrastructure.persistence;

import bo.phishshield.backend.empresa.domain.model.Empresa;
import bo.phishshield.backend.empresa.domain.port.out.EmpresaRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class EmpresaPersistenceAdapter implements EmpresaRepositoryPort {

    private final EmpresaJpaRepository jpaRepository;

    public EmpresaPersistenceAdapter(EmpresaJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    /** Ordenadas por nombre, para que la tabla y los selectores salgan siempre igual. */
    @Override
    public List<Empresa> listarTodas() {
        return jpaRepository.findAll()
                .stream()
                .map(this::aDominio)
                .sorted(Comparator.comparing(Empresa::getNombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public Optional<Empresa> buscarPorId(Long id) {
        return jpaRepository.findById(id).map(this::aDominio);
    }

    @Override
    public Optional<Empresa> buscarPorUuid(UUID uuidPublico) {
        return jpaRepository.findByUuidPublico(uuidPublico).map(this::aDominio);
    }

    /**
     * Si la empresa ya tiene id, se carga la fila y se actualiza. Antes se
     * armaba siempre una entidad nueva sin id, y JPA la habria insertado como
     * una empresa duplicada (con otro uuid) en vez de modificarla.
     */
    @Override
    @Transactional
    public Empresa guardar(Empresa empresa) {
        OffsetDateTime ahora = OffsetDateTime.now();
        EmpresaEntity entidad;

        if (empresa.getId() == null) {
            entidad = new EmpresaEntity();
            entidad.setUuidPublico(UUID.randomUUID());
            entidad.setCreadoEn(ahora);
        } else {
            entidad = jpaRepository.findById(empresa.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "No existe la empresa con id " + empresa.getId()));
        }

        entidad.setNombre(empresa.getNombre());
        entidad.setNit(empresa.getNit());
        entidad.setSector(empresa.getSector());
        entidad.setCiudad(empresa.getCiudad());
        entidad.setPlan(empresa.getPlan());
        entidad.setActiva(empresa.isActiva());
        entidad.setActualizadoEn(ahora);

        return aDominio(jpaRepository.save(entidad));
    }

    @Override
    public boolean existeNit(String nit) {
        return nit != null && jpaRepository.existsByNit(nit);
    }

    @Override
    public boolean existeNitEnOtra(String nit, Long idEmpresa) {
        return nit != null && jpaRepository.existsByNitAndIdEmpresaNot(nit, idEmpresa);
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
