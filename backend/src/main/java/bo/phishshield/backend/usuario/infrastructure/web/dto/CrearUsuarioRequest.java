package bo.phishshield.backend.usuario.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Entrada de POST /api/admin/usuarios. Quien puede dar cada rol y a que
 * empresa no se revisa aca: lo decide PoliticaGestionUsuarios (dominio).
 *
 * @param rol     ADMIN, ADMIN_EMPRESA o USUARIO
 * @param empresa id (uuid publico) de la empresa; el ADMIN_EMPRESA puede omitirlo
 */
public record CrearUsuarioRequest(

        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 80, message = "Los nombres no pueden pasar de 80 caracteres")
        String nombres,

        @Size(max = 80, message = "Los apellidos no pueden pasar de 80 caracteres")
        String apellidos,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato valido")
        @Size(max = 150, message = "El email no puede pasar de 150 caracteres")
        String email,

        @NotBlank(message = "La contrasena inicial es obligatoria")
        @Size(min = 8, max = 72, message = "La contrasena debe tener entre 8 y 72 caracteres")
        String contrasena,

        @NotBlank(message = "El rol es obligatorio")
        String rol,

        UUID empresa
) {
}
