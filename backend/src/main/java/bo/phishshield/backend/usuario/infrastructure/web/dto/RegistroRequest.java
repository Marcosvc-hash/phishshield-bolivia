package bo.phishshield.backend.usuario.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Entrada de POST /api/auth/registro. */
public record RegistroRequest(

        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 80, message = "Los nombres no pueden pasar de 80 caracteres")
        String nombres,

        @Size(max = 80, message = "Los apellidos no pueden pasar de 80 caracteres")
        String apellidos,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato valido")
        @Size(max = 150, message = "El email no puede pasar de 150 caracteres")
        String email,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 8, max = 72, message = "La contrasena debe tener entre 8 y 72 caracteres")
        String contrasena
) {
}
