package bo.phishshield.backend.analisis.domain.model;

/**
 * Un dominio legitimo de una entidad boliviana conocida.
 * Se lee de la tabla entidades_suplantadas y sirve de referencia
 * para detectar imitaciones.
 */
public record DominioOficial(String entidad, String dominio) {

    public DominioOficial {
        if (entidad == null || entidad.isBlank()) {
            throw new IllegalArgumentException("La entidad es obligatoria");
        }
        if (dominio == null || dominio.isBlank()) {
            throw new IllegalArgumentException("El dominio oficial es obligatorio");
        }
        dominio = dominio.toLowerCase().trim();
    }
}