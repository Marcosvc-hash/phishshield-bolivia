package bo.phishshield.backend.empresa.infrastructure.web;

import bo.phishshield.backend.empresa.application.ComandoCrearEmpresa;
import bo.phishshield.backend.empresa.application.CrearEmpresaUseCase;
import bo.phishshield.backend.empresa.domain.port.out.EmpresaRepositoryPort;
import bo.phishshield.backend.empresa.infrastructure.web.dto.CrearEmpresaRequest;
import bo.phishshield.backend.empresa.infrastructure.web.dto.EmpresaResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {

    private final CrearEmpresaUseCase crearEmpresa;
    private final EmpresaRepositoryPort empresaRepository;

    public EmpresaController(CrearEmpresaUseCase crearEmpresa,
                             EmpresaRepositoryPort empresaRepository) {
        this.crearEmpresa = crearEmpresa;
        this.empresaRepository = empresaRepository;
    }

    @GetMapping
    public List<EmpresaResponse> listar() {
        return empresaRepository.listarTodas()
                .stream()
                .map(EmpresaResponse::desde)
                .toList();
    }

    /** El "id" publico es el uuid_publico: el BIGINT interno nunca sale de la API. */
    @GetMapping("/{id}")
    public ResponseEntity<EmpresaResponse> obtener(@PathVariable UUID id) {
        return empresaRepository.buscarPorUuid(id)
                .map(EmpresaResponse::desde)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmpresaResponse crear(@Valid @RequestBody CrearEmpresaRequest request) {
        return EmpresaResponse.desde(crearEmpresa.ejecutar(new ComandoCrearEmpresa(
                request.nombre(),
                request.nit(),
                request.sector(),
                request.ciudad(),
                request.plan())));
    }
}
