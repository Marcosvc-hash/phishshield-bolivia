package bo.phishshield.backend.analisis.domain.model;

import java.util.Optional;

/**
 * Aviso que se genera cuando un analisis no es seguro.
 * Se persiste en la tabla alertas, una sola por analisis.
 */
public record Alerta(Severidad severidad, CanalAlerta canal) {

    public Alerta {
        if (severidad == null || canal == null) {
            throw new IllegalArgumentException("La alerta necesita severidad y canal");
        }
    }

    /** Vacio si el analisis es seguro; si no, la alerta que le corresponde. */
    public static Optional<Alerta> para(Analisis analisis) {
        if (!analisis.requiereAlerta()) {
            return Optional.empty();
        }
        return Optional.of(new Alerta(
                Severidad.desdePuntaje(analisis.getNivelRiesgo()),
                CanalAlerta.desdeOrigen(analisis.getOrigen())));
    }
}
