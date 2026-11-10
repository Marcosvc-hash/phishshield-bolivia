package bo.phishshield.backend.alerta.infrastructure.web.dto;

import bo.phishshield.backend.alerta.domain.model.AlertaGestionada;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de PATCH /api/admin/alertas/{id}. Los dos campos son opcionales,
 * pero al menos uno debe venir. Ejemplos:
 *   {"estado": "en_revision"}
 *   {"estado": "descartada", "notas": "Falso positivo: es la web del cliente"}
 *   {"notas": ""}   (borra las notas)
 *
 * Que transicion es valida NO se revisa aca: lo decide el dominio.
 */
public record ActualizarAlertaRequest(

        String estado,

        @Size(max = AlertaGestionada.MAXIMO_NOTAS,
                message = "Las notas no pueden superar los " + AlertaGestionada.MAXIMO_NOTAS + " caracteres")
        String notas
) {
}
