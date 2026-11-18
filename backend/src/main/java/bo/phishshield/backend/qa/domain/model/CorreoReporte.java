package bo.phishshield.backend.qa.domain.model;

import java.util.List;

/** El correo con los reportes adjuntos, listo para que lo envie el adaptador SMTP. */
public record CorreoReporte(CorreoDestino destino, String asunto, String cuerpo, List<Adjunto> adjuntos) {

    public CorreoReporte {
        if (destino == null || asunto == null || asunto.isBlank() || cuerpo == null) {
            throw new IllegalArgumentException("Faltan datos del correo");
        }
        adjuntos = adjuntos == null ? List.of() : List.copyOf(adjuntos);
    }

    /**
     * @param nombreArchivo como lo vera el destinatario, por ejemplo reporte-detallado.pdf
     * @param ruta          relativa a la carpeta de evidencias
     * @param tipoMime      application/pdf, text/html...
     */
    public record Adjunto(String nombreArchivo, String ruta, String tipoMime) {

        public Adjunto {
            if (nombreArchivo == null || nombreArchivo.isBlank() || ruta == null || ruta.isBlank()
                    || tipoMime == null || tipoMime.isBlank()) {
                throw new IllegalArgumentException("Faltan datos del adjunto");
            }
        }
    }
}
