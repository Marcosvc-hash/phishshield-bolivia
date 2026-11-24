package bo.phishshield.backend.reporte.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Cuerpo de POST /api/admin/reportes: {"idEmpresa": "...", "desde": "2026-11-01", "hasta": "2026-11-24"}.
 * El ADMIN_EMPRESA puede omitir idEmpresa (se usa la suya).
 */
public record GenerarReporteRequest(

        UUID idEmpresa,

        @NotNull(message = "Indique la fecha 'desde'")
        LocalDate desde,

        @NotNull(message = "Indique la fecha 'hasta'")
        LocalDate hasta
) {
}
