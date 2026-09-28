package bo.phishshield.backend.analisis.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Entrada del endpoint POST /api/analisis.
 * Mientras no exista autenticacion, el usuario y la empresa
 * llegan en el cuerpo; en el Sprint 3 saldran del token JWT.
 */
public record AnalizarUrlRequest(

        @NotBlank(message = "La URL es obligatoria")
        @Size(max = 2048, message = "La URL es demasiado larga")
        String url,

        String origen,

        Long idUsuario,

        Long idEmpresa
) {
}