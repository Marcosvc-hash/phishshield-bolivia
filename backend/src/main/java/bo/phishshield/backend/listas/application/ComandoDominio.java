package bo.phishshield.backend.listas.application;

/**
 * Entrada para agregar o editar un dominio.
 *
 * @param lista   blanca o negra (se ignora al editar: la lista no cambia)
 * @param entidad nombre exacto de la entidad suplantada; null o vacio = ninguna
 * @param ip      IP de la peticion, para la auditoria
 */
public record ComandoDominio(String dominio, String lista, String motivo, String entidad, String ip) {

    public ComandoDominio {
        entidad = entidad == null || entidad.isBlank() ? null : entidad.trim();
    }
}
