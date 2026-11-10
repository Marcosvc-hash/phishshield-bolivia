package bo.phishshield.backend.shared.infrastructure.persistence;

/**
 * Piezas de SQL que repiten los adaptadores JdbcClient de administracion.
 */
public final class FiltrosSql {

    /**
     * "Solo esta empresa, salvo que el parametro sea null". Con alcance global
     * el parametro va null y la condicion deja pasar todo. El CAST es
     * necesario porque PostgreSQL no adivina el tipo de un null.
     */
    public static final String DE_LA_EMPRESA = "(CAST(:empresa AS BIGINT) IS NULL OR %s = :empresa)";

    private FiltrosSql() {
        // clase de utilidad: no se instancia
    }

    /**
     * Patron para ILIKE que busca el texto en cualquier parte. Escapa los
     * comodines de LIKE: sin esto, buscar "100%" o "mi_sitio" daria
     * resultados de mas. Devuelve null si no hay texto (filtro apagado).
     */
    public static String contiene(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String escapado = texto.trim()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escapado + "%";
    }
}
