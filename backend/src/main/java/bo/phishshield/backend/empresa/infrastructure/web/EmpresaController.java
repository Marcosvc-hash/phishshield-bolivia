package bo.phishshield.backend.empresa.infrastructure.web;

import bo.phishshield.backend.empresa.domain.model.Empresa;
import bo.phishshield.backend.empresa.domain.port.out.EmpresaRepositoryPort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {

    private final EmpresaRepositoryPort empresaRepository;

    public EmpresaController(EmpresaRepositoryPort empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    @GetMapping
    public List<Empresa> listar() {
        return empresaRepository.listarTodas();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Empresa> obtener(@PathVariable Long id) {
        return empresaRepository.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}