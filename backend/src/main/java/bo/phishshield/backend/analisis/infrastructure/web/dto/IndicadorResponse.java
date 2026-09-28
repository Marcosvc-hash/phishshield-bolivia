package bo.phishshield.backend.analisis.infrastructure.web.dto;

import bo.phishshield.backend.analisis.domain.model.Indicador;

import java.math.BigDecimal;

public record IndicadorResponse(String codigo, String detalle, BigDecimal aporte) {

    public static IndicadorResponse desde(Indicador indicador) {
        return new IndicadorResponse(
                indicador.codigoRegla(),
                indicador.valor(),
                indicador.aporte());
    }
}