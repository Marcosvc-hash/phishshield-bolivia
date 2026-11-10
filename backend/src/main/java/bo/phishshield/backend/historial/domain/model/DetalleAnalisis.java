package bo.phishshield.backend.historial.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Todo lo que se muestra al abrir un analisis del historial.
 *
 * @param idEmpresa  BIGINT interno: solo sirve para revisar el alcance,
 *                   nunca sale de la API
 * @param modeloIa   "nombre version" del modelo consultado; null si no se uso
 * @param indicadores reglas que se dispararon, de mayor a menor aporte
 */
public record DetalleAnalisis(UUID id,
                              Long idEmpresa,
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
                              List<IndicadorDetalle> indicadores) {

    public DetalleAnalisis {
        indicadores = List.copyOf(indicadores);
    }

    /** Una regla disparada, con la descripcion que tiene en reglas_deteccion. */
    public record IndicadorDetalle(String codigo, String descripcion, String valor, BigDecimal aporte) {
    }
}
