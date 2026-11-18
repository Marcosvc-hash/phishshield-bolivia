package bo.phishshield.backend.qa.domain.model;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Lo que informa el ejecutor de pruebas cuando un caso termina: su estado,
 * por que fallo (si fallo), los pasos que alcanzo a registrar y sus metricas.
 *
 * NOT_RUN no es un desenlace: es lo que queda cuando el caso nunca llego
 * a correr, asi que el ejecutor no puede informarlo.
 */
public record DesenlaceCaso(
        String codigo,
        EstadoCaso estado,
        TipoFallo tipoFallo,
        String mensajeError,
        String pasoFallido,
        String observacion,
        OffsetDateTime inicio,
        OffsetDateTime fin,
        List<PasoEjecutado> pasos,
        List<MetricaRendimiento> metricas
) {

    /** Ancho de qa_resultados.paso_fallido. */
    public static final int MAXIMO_PASO_FALLIDO = 255;

    public DesenlaceCaso {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("El desenlace necesita el código del caso");
        }
        if (estado == null || estado == EstadoCaso.NOT_RUN) {
            throw new IllegalArgumentException("Un caso que terminó solo puede quedar en PASS, FAIL o SKIPPED");
        }
        if ((estado == EstadoCaso.FAIL) != (tipoFallo != null)) {
            throw new IllegalArgumentException("El tipo de fallo va solo, y siempre, en los casos FAIL");
        }
        if (inicio == null || fin == null) {
            throw new IllegalArgumentException("El desenlace de " + codigo + " necesita inicio y fin");
        }
        if (fin.isBefore(inicio)) {
            throw new IllegalArgumentException("El caso " + codigo + " no puede terminar antes de empezar");
        }
        mensajeError = Textos.mensaje(mensajeError);
        pasoFallido = Textos.recortar(Textos.opcional(pasoFallido), MAXIMO_PASO_FALLIDO);
        observacion = Textos.mensaje(observacion);
        pasos = pasos == null ? List.of() : List.copyOf(pasos);
        metricas = metricas == null ? List.of() : List.copyOf(metricas);
    }

    public static DesenlaceCaso aprobado(String codigo, OffsetDateTime inicio, OffsetDateTime fin,
                                         List<PasoEjecutado> pasos, List<MetricaRendimiento> metricas) {
        return new DesenlaceCaso(codigo, EstadoCaso.PASS, null, null, null, null, inicio, fin, pasos, metricas);
    }

    public static DesenlaceCaso fallido(String codigo, TipoFallo tipoFallo, String mensajeError, String pasoFallido,
                                        OffsetDateTime inicio, OffsetDateTime fin,
                                        List<PasoEjecutado> pasos, List<MetricaRendimiento> metricas) {
        return new DesenlaceCaso(codigo, EstadoCaso.FAIL, tipoFallo, mensajeError, pasoFallido, null,
                inicio, fin, pasos, metricas);
    }

    public static DesenlaceCaso omitido(String codigo, String motivo, OffsetDateTime inicio, OffsetDateTime fin,
                                        List<PasoEjecutado> pasos) {
        return new DesenlaceCaso(codigo, EstadoCaso.SKIPPED, null, null, null, motivo, inicio, fin, pasos, List.of());
    }
}
