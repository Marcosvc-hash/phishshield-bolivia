package bo.phishshield.backend.analisis.domain.model;

/**
 * Un dominio reportado como malicioso.
 * Se lee de la tabla dominios_lista (lista = 'negra') y una coincidencia
 * basta para que el analisis llegue a phishing.
 */
public record DominioListado(String dominio, String motivo) {

    public DominioListado {
        if (dominio == null || dominio.isBlank()) {
            throw new IllegalArgumentException("El dominio listado es obligatorio");
        }
        dominio = dominio.toLowerCase().trim();
    }
}
