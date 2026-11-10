package bo.phishshield.backend.panel.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Una fila de la vista v_resumen_empresa (solo la ve el ADMIN). */
public record ResumenEmpresa(String empresa,
                             long totalAnalisis,
                             long totalPhishing,
                             BigDecimal riesgoPromedio,
                             OffsetDateTime ultimoAnalisis) {
}
