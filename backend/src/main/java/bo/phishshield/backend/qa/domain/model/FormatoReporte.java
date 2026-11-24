package bo.phishshield.backend.qa.domain.model;

/** Los dos reportes de una ejecucion: el PDF detallado y el HTML resumido. */
public enum FormatoReporte {

    PDF("application/pdf", "reporte-detallado.pdf"),
    HTML("text/html; charset=UTF-8", "reporte-resumen.html");

    private final String tipoMime;
    private final String nombreArchivo;

    FormatoReporte(String tipoMime, String nombreArchivo) {
        this.tipoMime = tipoMime;
        this.nombreArchivo = nombreArchivo;
    }

    public String tipoMime() {
        return tipoMime;
    }

    /** Nombre con el que se guarda dentro de la carpeta de la ejecucion. */
    public String nombreArchivo() {
        return nombreArchivo;
    }
}
