package bo.phishshield.backend.alerta.domain.model;

import java.util.Set;

/**
 * Ciclo de vida de una alerta (chk_alertas_estado):
 *
 *   pendiente --> en_revision --> revisada
 *                             \-> descartada
 *
 * revisada y descartada son finales: una alerta cerrada ya no cambia.
 * Esta es la UNICA definicion de las transiciones validas; el controlador
 * y el frontend no las repiten como regla.
 */
public enum EstadoAlerta {

    PENDIENTE,
    EN_REVISION,
    REVISADA,
    DESCARTADA;

    public boolean puedePasarA(EstadoAlerta destino) {
        return siguientes().contains(destino);
    }

    public Set<EstadoAlerta> siguientes() {
        return switch (this) {
            case PENDIENTE -> Set.of(EN_REVISION);
            case EN_REVISION -> Set.of(REVISADA, DESCARTADA);
            case REVISADA, DESCARTADA -> Set.of();
        };
    }

    public boolean esFinal() {
        return siguientes().isEmpty();
    }

    public String aValorBd() {
        return name().toLowerCase();
    }

    public static EstadoAlerta desdeValorBd(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El estado de la alerta es obligatorio");
        }
        try {
            return valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Estado de alerta invalido: " + valor
                    + " (use pendiente, en_revision, revisada o descartada)");
        }
    }
}
