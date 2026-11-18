package bo.phishshield.backend.qa.domain.model;

/**
 * Los dos reportes generados para una ejecucion.
 *
 * @param rutaPdf  reporte detallado, relativo a la carpeta de evidencias
 * @param rutaHtml reporte resumido (HTML independiente), relativo a la misma carpeta
 */
public record ArchivosReporte(String rutaPdf, String rutaHtml) {

    public ArchivosReporte {
        if (rutaPdf == null || rutaPdf.isBlank() || rutaHtml == null || rutaHtml.isBlank()) {
            throw new IllegalArgumentException("Faltan las rutas de los reportes");
        }
    }
}
