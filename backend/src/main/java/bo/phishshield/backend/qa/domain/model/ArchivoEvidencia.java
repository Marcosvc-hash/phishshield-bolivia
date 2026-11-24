package bo.phishshield.backend.qa.domain.model;

/**
 * Un archivo leido de la carpeta de evidencias (reporte o captura), listo
 * para entregarlo por la API.
 */
public record ArchivoEvidencia(String nombre, String tipoMime, byte[] contenido) {

    public ArchivoEvidencia {
        if (nombre == null || nombre.isBlank() || tipoMime == null || contenido == null) {
            throw new IllegalArgumentException("El archivo de evidencia necesita nombre, tipo y contenido");
        }
    }
}
