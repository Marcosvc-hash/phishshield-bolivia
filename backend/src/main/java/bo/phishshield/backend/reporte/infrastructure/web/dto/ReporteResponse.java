package bo.phishshield.backend.reporte.infrastructure.web.dto;

import bo.phishshield.backend.reporte.domain.model.ReporteEmpresa;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static bo.phishshield.backend.shared.infrastructure.web.dto.FechasLocales.enZonaLocal;

/** Un reporte por empresa. "id" es reportes.uuid_publico (V14); el BIGINT no sale de la API. */
public record ReporteResponse(
        UUID id,
        UUID idEmpresa,
        String empresa,
        LocalDate desde,
        LocalDate hasta,
        long dias,
        int totalAnalisis,
        int totalPhishing,
        int totalAlertas,
        BigDecimal porcentajePhishing,
        String generadoPor,
        OffsetDateTime fechaGeneracion
) {

    public static ReporteResponse desde(ReporteEmpresa r) {
        return new ReporteResponse(
                r.uuidPublico(), r.empresa().uuidPublico(), r.empresa().nombre(),
                r.periodo().desde(), r.periodo().hasta(), r.periodo().dias(),
                r.totales().analisis(), r.totales().phishing(), r.totales().alertas(),
                r.totales().porcentajePhishing(), r.generadoPor(), enZonaLocal(r.fechaGeneracion()));
    }
}
