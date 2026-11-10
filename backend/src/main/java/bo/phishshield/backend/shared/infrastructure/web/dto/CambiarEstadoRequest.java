package bo.phishshield.backend.shared.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;

/** Cuerpo comun de los PATCH .../estado (usuarios, empresas, dominios): {"activo": true|false}. */
public record CambiarEstadoRequest(

        @NotNull(message = "Indique si queda activo (true) o inactivo (false)")
        Boolean activo
) {
}
