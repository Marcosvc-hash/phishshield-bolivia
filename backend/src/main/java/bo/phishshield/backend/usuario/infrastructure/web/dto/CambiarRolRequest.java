package bo.phishshield.backend.usuario.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Entrada de PATCH /api/admin/usuarios/{id}/rol: {"rol": "ADMIN_EMPRESA"}. */
public record CambiarRolRequest(

        @NotBlank(message = "Indique el rol nuevo (ADMIN, ADMIN_EMPRESA o USUARIO)")
        String rol
) {
}
