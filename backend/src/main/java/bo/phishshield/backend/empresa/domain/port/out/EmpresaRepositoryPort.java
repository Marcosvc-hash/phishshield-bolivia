package bo.phishshield.backend.empresa.domain.port.out;

import bo.phishshield.backend.empresa.domain.model.Empresa;

import java.util.List;
import java.util.Optional;

public interface EmpresaRepositoryPort {

    List<Empresa> listarTodas();

    Optional<Empresa> buscarPorId(Long id);
}
