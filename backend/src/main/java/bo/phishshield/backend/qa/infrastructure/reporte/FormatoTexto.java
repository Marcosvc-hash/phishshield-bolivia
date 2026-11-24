package bo.phishshield.backend.qa.infrastructure.reporte;

import bo.phishshield.backend.qa.domain.model.EstadoCaso;
import bo.phishshield.backend.qa.domain.model.EstadoEjecucion;
import bo.phishshield.backend.qa.domain.model.Herramienta;
import bo.phishshield.backend.qa.domain.model.MetricaRendimiento;
import bo.phishshield.backend.qa.domain.model.ResultadoGlobal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Formatos comunes del PDF y del HTML: fechas en hora de Bolivia, duraciones y etiquetas. */
final class FormatoTexto {

    static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private FormatoTexto() {
    }

    static String fecha(OffsetDateTime fecha) {
        return fecha == null ? "-" : fecha.atZoneSameInstant(ZONA).format(FECHA_HORA);
    }

    /** 740 ms, 12,4 s, 3 min 05 s. */
    static String duracion(Long ms) {
        if (ms == null) {
            return "-";
        }
        if (ms < 1000) {
            return ms + " ms";
        }
        if (ms < 60_000) {
            return BigDecimal.valueOf(ms).divide(BigDecimal.valueOf(1000), 1, RoundingMode.HALF_UP)
                    .toPlainString().replace('.', ',') + " s";
        }
        long segundos = ms / 1000;
        return (segundos / 60) + " min " + String.format("%02d", segundos % 60) + " s";
    }

    static String estadoEjecucion(EstadoEjecucion estado) {
        return switch (estado) {
            case PENDIENTE -> "Pendiente";
            case EN_CURSO -> "En curso";
            case FINALIZADA -> "Finalizada";
            case FALLIDA -> "Interrumpida";
        };
    }

    static String resultadoGlobal(ResultadoGlobal resultado, EstadoEjecucion estado) {
        if (resultado != null) {
            return resultado.etiqueta();
        }
        return estado == EstadoEjecucion.FALLIDA ? "Sin resultado (interrumpida)" : "Sin resultado";
    }

    /** PASS, FAIL, SKIPPED o NOT_RUN, como se usan en testing. */
    static String estadoCaso(EstadoCaso estado) {
        return estado.name();
    }

    static String herramienta(Herramienta herramienta) {
        return herramienta.etiqueta();
    }

    /** Color de cada estado (RGB hex, sin #), con la paleta del frontend. */
    static String colorEstado(EstadoCaso estado) {
        return switch (estado) {
            case PASS -> "15803d";
            case FAIL -> "b91c1c";
            case SKIPPED -> "a14a06";
            case NOT_RUN -> "64748b";
        };
    }

    static String colorResultado(ResultadoGlobal resultado) {
        if (resultado == null) {
            return "64748b";
        }
        return switch (resultado) {
            case APROBADA -> "15803d";
            case CON_FALLOS -> "b91c1c";
            case INCOMPLETA -> "a14a06";
        };
    }

    static String valorMetrica(MetricaRendimiento m) {
        return numero(m.valor()) + " " + m.unidad();
    }

    static String umbralMetrica(MetricaRendimiento m) {
        return m.umbral() == null ? "-" : numero(m.umbral()) + " " + m.unidad();
    }

    static String cumpleMetrica(MetricaRendimiento m) {
        return m.cumple() == null ? "Informativa" : (m.cumple() ? "Cumple" : "No cumple");
    }

    static String numero(BigDecimal valor) {
        return valor == null ? "-" : valor.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    static String vacioComoGuion(String texto) {
        return texto == null || texto.isBlank() ? "-" : texto;
    }
}
