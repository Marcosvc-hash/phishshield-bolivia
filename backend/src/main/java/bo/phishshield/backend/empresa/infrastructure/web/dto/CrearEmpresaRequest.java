package bo.phishshield.backend.empresa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CrearEmpresaRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        String nombre,

        @Pattern(regexp = "^[0-9]{7,15}$", message = "El NIT debe tener entre 7 y 15 digitos")
        String nit,

        @Size(max = 60, message = "El sector no puede superar los 60 caracteres")
        String sector,

        @Size(max = 60, message = "La ciudad no puede superar los 60 caracteres")
        String ciudad,

        @Pattern(regexp = "basico|profesional|empresarial",
                message = "El plan debe ser basico, profesional o empresarial")
        String plan
) {
}