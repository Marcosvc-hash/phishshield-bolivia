package bo.phishshield.backend.usuario.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Entrada de POST /api/auth/refresh y /api/auth/logout. */
public record RefrescoRequest(

        @NotBlank(message = "El token de refresco es obligatorio")
        String tokenRefresco
) {
}
