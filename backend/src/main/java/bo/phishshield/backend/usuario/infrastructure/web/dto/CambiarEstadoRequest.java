package bo.phishshield.backend.usuario.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;

/** Entrada de PATCH /api/admin/usuarios/{id}/estado. */
public record CambiarEstadoRequest(

        @NotNull(message = "Indique si la cuenta queda activa (true) o inactiva (false)")
        Boolean activo
) {
}
