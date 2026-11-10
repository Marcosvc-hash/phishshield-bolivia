package bo.phishshield.backend.empresa.domain.port.out;

import bo.phishshield.backend.empresa.domain.model.Empresa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmpresaRepositoryPort {

    List<Empresa> listarTodas();

    Optional<Empresa> buscarPorId(Long id);

    Optional<Empresa> buscarPorUuid(UUID uuidPublico);

    /** Inserta si no tiene id; si lo tiene, actualiza la fila existente. */
    Empresa guardar(Empresa empresa);

    boolean existeNit(String nit);

    /** Si otra empresa (no la indicada) ya usa ese NIT. Sirve al editar. */
    boolean existeNitEnOtra(String nit, Long idEmpresa);
}
