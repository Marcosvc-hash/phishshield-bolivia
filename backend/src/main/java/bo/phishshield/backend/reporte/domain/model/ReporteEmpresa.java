package bo.phishshield.backend.reporte.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Un reporte guardado (tabla reportes): de que empresa, que periodo, sus
 * totales, quien lo genero y cuando. Hay uno solo por empresa y periodo:
 * generarlo otra vez recalcula los totales (no crea otro).
 *
 * @param id          BIGINT interno; no sale de la API
 * @param generadoPor correo de quien lo genero (null si esa cuenta ya no existe)
 */
public record ReporteEmpresa(
        Long id,
        UUID uuidPublico,
        EmpresaDelReporte empresa,
        PeriodoReporte periodo,
        TotalesReporte totales,
        String generadoPor,
        OffsetDateTime fechaGeneracion
) {

    public ReporteEmpresa {
        if (uuidPublico == null || empresa == null || periodo == null || totales == null || fechaGeneracion == null) {
            throw new IllegalArgumentException("Faltan datos del reporte");
        }
    }
}
