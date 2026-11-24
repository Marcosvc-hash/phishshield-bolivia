package bo.phishshield.backend.qa.infrastructure.web.dto;

import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * Cuerpo de POST /api/admin/qa/ejecuciones. Dos formas:
 *
 *   {"suite": "smoke"}                                     la suite completa
 *   {"codigos": ["TC-SAN-001", "TC-SAN-003"]}              casos marcados (todos de una suite)
 *
 * correo es opcional (por defecto, el de quien la lanza) y parametros
 * ajusta las pruebas no funcionales, por ejemplo {"usuarios": "10"}.
 */
public record IniciarEjecucionRequest(

        String suite,

        @Size(max = 50, message = "Se pueden marcar como máximo 50 casos")
        List<String> codigos,

        @Size(max = 150, message = "El correo no puede superar los 150 caracteres")
        String correo,

        Map<String, String> parametros
) {
}
