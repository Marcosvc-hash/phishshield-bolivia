package bo.phishshield.backend.alerta.application;

import bo.phishshield.backend.alerta.domain.model.EstadoAlerta;

import java.util.UUID;

/**
 * Entrada de ActualizarAlertaUseCase.
 *
 * @param estadoNuevo null = no cambiar el estado
 * @param notas       null = no tocar las notas; "" = borrarlas
 * @param ip          IP de la peticion, para la auditoria
 */
public record ComandoActualizarAlerta(UUID id, EstadoAlerta estadoNuevo, String notas, String ip) {

    public ComandoActualizarAlerta {
        if (id == null) {
            throw new IllegalArgumentException("Falta el id de la alerta");
        }
        if (estadoNuevo == null && notas == null) {
            throw new IllegalArgumentException("Indique un estado nuevo o las notas");
        }
    }
}
