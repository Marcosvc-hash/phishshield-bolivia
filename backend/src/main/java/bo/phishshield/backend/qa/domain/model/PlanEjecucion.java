package bo.phishshield.backend.qa.domain.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lo que necesita el ejecutor de pruebas: que casos correr, para que
 * ejecucion (las evidencias se guardan en una carpeta por ejecucion) y con
 * que parametros (usuarios concurrentes, umbral de p95, minutos de soak...).
 */
public record PlanEjecucion(UUID idEjecucion, Suite suite, List<String> codigos, Map<String, String> parametros) {

    public PlanEjecucion {
        if (idEjecucion == null || suite == null || codigos == null || codigos.isEmpty()) {
            throw new IllegalArgumentException("El plan necesita la ejecución, la suite y al menos un caso");
        }
        codigos = List.copyOf(codigos);
        parametros = parametros == null ? Map.of() : Map.copyOf(parametros);
    }

    public static PlanEjecucion de(Ejecucion ejecucion) {
        return new PlanEjecucion(ejecucion.getUuidPublico(), ejecucion.getSuite(),
                ejecucion.codigos(), ejecucion.getParametros());
    }
}
