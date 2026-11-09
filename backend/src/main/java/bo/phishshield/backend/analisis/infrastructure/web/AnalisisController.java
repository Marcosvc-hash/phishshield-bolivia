package bo.phishshield.backend.analisis.infrastructure.web;

import bo.phishshield.backend.analisis.application.AnalizarUrlUseCase;
import bo.phishshield.backend.analisis.application.ComandoAnalizarUrl;
import bo.phishshield.backend.analisis.domain.model.Analisis;
import bo.phishshield.backend.analisis.domain.model.OrigenAnalisis;
import bo.phishshield.backend.analisis.domain.port.out.AnalisisRepositoryPort;
import bo.phishshield.backend.analisis.infrastructure.web.dto.AnalisisResponse;
import bo.phishshield.backend.analisis.infrastructure.web.dto.AnalizarUrlRequest;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/analisis")
public class AnalisisController {

    private static final int LIMITE_POR_DEFECTO = 20;
    private static final int LIMITE_MAXIMO = 100;

    private final AnalizarUrlUseCase analizarUrl;
    private final AnalisisRepositoryPort analisisRepository;

    public AnalisisController(AnalizarUrlUseCase analizarUrl,
                              AnalisisRepositoryPort analisisRepository) {
        this.analizarUrl = analizarUrl;
        this.analisisRepository = analisisRepository;
    }

    /**
     * Devuelve 200 y no 201 a proposito: analizar dos veces la misma URL
     * no crea un recurso nuevo, devuelve el analisis ya existente.
     *
     * Es publico. Si llega con token, el analisis queda a nombre del usuario
     * y de su empresa; sin token, queda anonimo. El usuario nunca sale del
     * cuerpo: si saliera, cualquiera podria atribuirle analisis a otro.
     */
    @PostMapping
    public AnalisisResponse analizar(@Valid @RequestBody AnalizarUrlRequest peticion,
                                     @AuthenticationPrincipal UsuarioActual quien) {

        OrigenAnalisis origen = peticion.origen() == null || peticion.origen().isBlank()
                ? OrigenAnalisis.WEB
                : OrigenAnalisis.desdeValorBd(peticion.origen());

        Analisis resultado = analizarUrl.ejecutar(new ComandoAnalizarUrl(
                peticion.url(),
                origen,
                quien == null ? null : quien.idUsuario(),
                quien == null ? null : quien.idEmpresa()));

        return AnalisisResponse.desde(resultado);
    }

    @GetMapping
    public List<AnalisisResponse> listar(
            @RequestParam(required = false) Integer limite) {

        int cantidad = limite == null
                ? LIMITE_POR_DEFECTO
                : Math.min(Math.max(1, limite), LIMITE_MAXIMO);

        return analisisRepository.listarUltimos(cantidad)
                .stream()
                .map(AnalisisResponse::desde)
                .toList();
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<AnalisisResponse> obtener(@PathVariable UUID uuid) {
        return analisisRepository.buscarPorUuid(uuid)
                .map(AnalisisResponse::desde)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
