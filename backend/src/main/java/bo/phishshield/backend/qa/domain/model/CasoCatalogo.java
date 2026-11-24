package bo.phishshield.backend.qa.domain.model;

/**
 * Un caso del catalogo tal como lo ve la pantalla: sus metadatos y si ya
 * tiene una prueba programada (si no, al ejecutarlo queda NOT_RUN).
 */
public record CasoCatalogo(CasoPrueba caso, boolean implementado) {

    public CasoCatalogo {
        if (caso == null) {
            throw new IllegalArgumentException("Falta el caso del catálogo");
        }
    }
}
