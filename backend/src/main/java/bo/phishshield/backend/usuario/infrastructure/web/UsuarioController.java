package bo.phishshield.backend.usuario.infrastructure.web;

import bo.phishshield.backend.usuario.domain.port.out.UsuarioRepositoryPort;
import bo.phishshield.backend.usuario.infrastructure.web.dto.UsuarioResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioRepositoryPort usuarioRepository;

    public UsuarioController(UsuarioRepositoryPort usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public List<UsuarioResponse> listar(@RequestParam(required = false) Long empresa) {
        var usuarios = empresa == null
                ? usuarioRepository.listarTodos()
                : usuarioRepository.listarPorEmpresa(empresa);

        return usuarios.stream()
                .map(UsuarioResponse::desde)
                .toList();
    }

    /** El "id" publico es el uuid_publico, igual que el que devuelve UsuarioResponse. */
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> obtener(@PathVariable UUID id) {
        return usuarioRepository.buscarPorUuid(id)
                .map(UsuarioResponse::desde)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** La base guarda los emails en minusculas (chk en usuarios), asi que se normaliza. */
    @GetMapping("/buscar")
    public ResponseEntity<UsuarioResponse> buscarPorEmail(@RequestParam String email) {
        return usuarioRepository.buscarPorEmail(email.trim().toLowerCase(Locale.ROOT))
                .map(UsuarioResponse::desde)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}