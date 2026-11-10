package bo.phishshield.backend.historial.infrastructure.web.dto;

import bo.phishshield.backend.historial.domain.model.DetalleAnalisis;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static bo.phishshield.backend.shared.infrastructure.web.dto.FechasLocales.enZonaLocal;

/** Detalle de un analisis. idEmpresa (BIGINT) se queda afuera a proposito. */
public record DetalleAnalisisResponse(
        UUID id,
        String url,
        String dominio,
        String resultado,
        BigDecimal nivelRiesgo,
        String origen,
        Integer tiempoMs,
        OffsetDateTime fecha,
        String usuario,
        String empresa,
        String modeloIa,
        String estadoAlerta,
        String severidadAlerta,
        List<Indicador> indicadores
) {

    public record Indicador(String codigo, String descripcion, String valor, BigDecimal aporte) {
    }

    public static DetalleAnalisisResponse desde(DetalleAnalisis d) {
        return new DetalleAnalisisResponse(
                d.id(),
                d.url(),
                d.dominio(),
                d.resultado(),
                d.nivelRiesgo(),
                d.origen(),
                d.tiempoMs(),
                enZonaLocal(d.fecha()),
                d.usuario(),
                d.empresa(),
                d.modeloIa(),
                d.estadoAlerta(),
                d.severidadAlerta(),
                d.indicadores().stream()
                        .map(i -> new Indicador(i.codigo(), i.descripcion(), i.valor(), i.aporte()))
                        .toList());
    }
}
