package bo.phishshield.backend.analisis.domain.model;

import java.math.BigDecimal;

/**
 * Una regla que se disparo durante el analisis:
 * que regla fue, que encontro, y cuanto sumo al nivel de riesgo.
 * Se persiste en la tabla analisis_indicadores.
 */
public record Indicador(String codigoRegla, String valor, BigDecimal aporte) {

    private static final BigDecimal LIMITE = new BigDecimal("100");

    public Indicador {
        if (codigoRegla == null || codigoRegla.isBlank()) {
            throw new IllegalArgumentException("El indicador necesita un codigo de regla");
        }
        if (aporte == null) {
            throw new IllegalArgumentException("El aporte no puede ser nulo");
        }
        if (aporte.abs().compareTo(LIMITE) > 0) {
            throw new IllegalArgumentException("El aporte debe estar entre -100 y 100");
        }
        if (valor != null && valor.length() > 255) {
            valor = valor.substring(0, 255);   // la columna es VARCHAR(255)
        }
    }
}