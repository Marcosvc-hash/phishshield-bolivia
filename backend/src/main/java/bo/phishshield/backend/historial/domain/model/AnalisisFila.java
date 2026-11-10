package bo.phishshield.backend.historial.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Una fila del historial.
 *
 * @param usuario      email de quien analizo; null si fue anonimo
 * @param estadoAlerta estado de la alerta del analisis; null si fue seguro
 */
public record AnalisisFila(UUID id,
                           String url,
                           String dominio,
                           String resultado,
                           BigDecimal nivelRiesgo,
                           OffsetDateTime fecha,
                           String usuario,
                           String empresa,
                           String estadoAlerta) {
}
