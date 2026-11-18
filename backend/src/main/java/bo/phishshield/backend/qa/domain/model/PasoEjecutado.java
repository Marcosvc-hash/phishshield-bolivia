package bo.phishshield.backend.qa.domain.model;

import java.time.OffsetDateTime;

/**
 * Un paso dentro de un caso, con lo que se observo y su evidencia
 * (tabla qa_pasos).
 *
 * @param rutaCaptura relativa a la carpeta de evidencias; null si el paso no tiene captura (API)
 * @param detalleApi  peticion y respuesta resumidas en pasos de API; null en pasos de interfaz
 */
public record PasoEjecutado(
        int numero,
        String descripcion,
        String resultadoObservado,
        EstadoPaso estado,
        String rutaCaptura,
        String detalleApi,
        OffsetDateTime registradoEn
) {

    /** Ancho de qa_pasos.descripcion. */
    public static final int MAXIMO_DESCRIPCION = 255;

    public PasoEjecutado {
        if (numero < 1) {
            throw new IllegalArgumentException("Los pasos se numeran desde 1");
        }
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("El paso " + numero + " necesita una descripción");
        }
        if (estado == null || registradoEn == null) {
            throw new IllegalArgumentException("El paso " + numero + " necesita estado y fecha");
        }
        descripcion = Textos.recortar(descripcion.strip(), MAXIMO_DESCRIPCION);
        resultadoObservado = Textos.opcional(resultadoObservado);
        rutaCaptura = Textos.opcional(rutaCaptura);
        detalleApi = Textos.opcional(detalleApi);
    }

    public boolean tieneCaptura() {
        return rutaCaptura != null;
    }

    public boolean fallo() {
        return estado == EstadoPaso.FALLO;
    }
}
