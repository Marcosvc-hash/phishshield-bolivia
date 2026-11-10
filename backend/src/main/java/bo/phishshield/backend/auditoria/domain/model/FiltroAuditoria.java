package bo.phishshield.backend.auditoria.domain.model;

import bo.phishshield.backend.shared.domain.model.RangoFechas;

/**
 * Filtros del registro de accesos. Todos opcionales.
 *
 * @param accion  accion exacta (LOGIN_FALLIDO, DOMINIO_AGREGADO...); null = todas
 * @param usuario parte del email; tambien encuentra los intentos con emails
 *                que no estan registrados (quedan en el detalle)
 */
public record FiltroAuditoria(String accion, String usuario, RangoFechas fechas) {

    public FiltroAuditoria {
        accion = accion == null || accion.isBlank() ? null : accion.trim().toUpperCase();
        usuario = usuario == null || usuario.isBlank() ? null : usuario.trim().toLowerCase();
        if (fechas == null) {
            fechas = RangoFechas.sinLimites();
        }
    }
}
