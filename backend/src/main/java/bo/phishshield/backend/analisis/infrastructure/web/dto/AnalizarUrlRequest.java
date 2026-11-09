package bo.phishshield.backend.analisis.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Entrada del endpoint POST /api/analisis.
 * El usuario y la empresa salen del token JWT, no del cuerpo.
 *
 * @param idUsuario obsoleto: se acepta para no romper clientes viejos, pero se ignora
 * @param idEmpresa obsoleto: se acepta para no romper clientes viejos, pero se ignora
 */
public record AnalizarUrlRequest(

        @NotBlank(message = "La URL es obligatoria")
        @Size(max = 2048, message = "La URL es demasiado larga")
        String url,

        @Pattern(regexp = "extension|plugin_correo|web|api",
                message = "El origen debe ser extension, plugin_correo, web o api")
        String origen,

        @Deprecated
        Long idUsuario,

        @Deprecated
        Long idEmpresa
) {
}
