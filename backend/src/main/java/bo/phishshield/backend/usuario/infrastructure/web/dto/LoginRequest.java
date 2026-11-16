package bo.phishshield.backend.usuario.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Entrada de POST /api/auth/login. */
public record LoginRequest(

        @NotBlank(message = "El email es obligatorio")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        String contrasena
) {
}
