package bo.phishshield.backend.qa.domain.model;

/**
 * Una URL del lote de robustez (tabla qa_lote_robustez) con su etiqueta
 * real: si es phishing o legitima. Con el lote se miden la precision y el
 * recall del sistema.
 */
public record UrlEtiquetada(String url, boolean esPhishing, String categoria) {

    public UrlEtiquetada {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("La URL del lote no puede estar vacía");
        }
        categoria = categoria == null || categoria.isBlank() ? "sin_categoria" : categoria.strip();
    }
}
