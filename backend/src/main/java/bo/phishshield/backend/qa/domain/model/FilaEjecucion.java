package bo.phishshield.backend.qa.domain.model;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Una fila del historial de ejecuciones: lo necesario para la tabla, sin
 * cargar los pasos ni las metricas de cada caso.
 */
public record FilaEjecucion(
        UUID id,
        Suite suite,
        AlcanceEjecucion alcance,
        String correo,
        EstadoEjecucion estado,
        ResultadoGlobal resultadoGlobal,
        OffsetDateTime creadaEn,
        OffsetDateTime inicio,
        OffsetDateTime fin,
        ResumenEjecucion resumen,
        EstadoCorreo estadoCorreo
) {

    public TipoPrueba tipo() {
        return suite.tipo();
    }

    public Long duracionMs() {
        return inicio == null || fin == null ? null : Duration.between(inicio, fin).toMillis();
    }
}
