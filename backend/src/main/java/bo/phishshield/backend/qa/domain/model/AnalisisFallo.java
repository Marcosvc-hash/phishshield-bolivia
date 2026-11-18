package bo.phishshield.backend.qa.domain.model;

/**
 * La causa probable de un FAIL segun el asistente de IA, con la evidencia
 * que la respalda. Es una ayuda para leer el fallo: el estado PASS/FAIL lo
 * decide siempre la prueba, nunca la IA.
 */
public record AnalisisFallo(String causaProbable, String evidencia) {

    public AnalisisFallo {
        if (causaProbable == null || causaProbable.isBlank()) {
            throw new IllegalArgumentException("El análisis de la IA no trae causa probable");
        }
        causaProbable = Textos.mensaje(causaProbable);
        evidencia = Textos.mensaje(evidencia);
    }
}
