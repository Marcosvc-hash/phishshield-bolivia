package bo.phishshield.backend.panel.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Una fila de "ultimos analisis". Es un modelo de lectura: trae el email y
 * la empresa ya resueltos, en vez del agregado Analisis completo.
 *
 * @param usuario email de quien lo pidio; null si fue anonimo
 */
public record AnalisisReciente(UUID id,
                               String contenido,
                               String dominio,
                               String resultado,
                               BigDecimal nivelRiesgo,
                               OffsetDateTime fecha,
                               String usuario,
                               String empresa) {
}
