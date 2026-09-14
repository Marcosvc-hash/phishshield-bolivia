package bo.phishshield.backend.empresa.infrastructure.web;

import bo.phishshield.backend.empresa.domain.model.Empresa;
import bo.phishshield.backend.empresa.domain.port.out.EmpresaRepositoryPort;
import bo.phishshield.backend.empresa.infrastructure.web.dto.CrearEmpresaRequest;
import bo.phishshield.backend.empresa.infrastructure.web.dto.EmpresaResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {

    private final EmpresaRepositoryPort empresaRepository;

    public EmpresaController(EmpresaRepositoryPort empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    @GetMapping
    public List<EmpresaResponse> listar() {
        return empresaRepository.listarTodas()
                .stream()
                .map(EmpresaResponse::desde)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmpresaResponse> obtener(@PathVariable Long id) {
        return empresaRepository.buscarPorId(id)
                .map(EmpresaResponse::desde)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmpresaResponse crear(@Valid @RequestBody CrearEmpresaRequest request) {

        if (empresaRepository.existeNit(request.nit())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe una empresa registrada con el NIT " + request.nit());
        }

        Empresa nueva = Empresa.nueva(
                request.nombre(),
                request.nit(),
                request.sector(),
                request.ciudad(),
                request.plan()
        );

        return EmpresaResponse.desde(empresaRepository.guardar(nueva));
    }
}