package bo.phishshield.backend.qa.domain.model;

import java.util.List;

/**
 * Lo que recibe el asistente de IA para proponer la causa de un FAIL:
 * el error, el paso donde ocurrio y los pasos registrados (el "log").
 */
public record ContextoFallo(
        String codigo,
        String nombreCaso,
        TipoFallo tipoFallo,
        String mensajeError,
        String pasoFallido,
        List<PasoEjecutado> pasos
) {

    public ContextoFallo {
        pasos = pasos == null ? List.of() : List.copyOf(pasos);
    }

    public static ContextoFallo de(ResultadoCaso resultado) {
        if (resultado.getEstado() != EstadoCaso.FAIL) {
            throw new IllegalArgumentException("Solo se analizan casos en FAIL");
        }
        return new ContextoFallo(resultado.getCodigo(), resultado.getCaso().nombre(), resultado.getTipoFallo(),
                resultado.getMensajeError(), resultado.getPasoFallido(), resultado.getPasos());
    }
}
