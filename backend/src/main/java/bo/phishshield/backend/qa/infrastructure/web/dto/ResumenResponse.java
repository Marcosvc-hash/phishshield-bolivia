package bo.phishshield.backend.qa.infrastructure.web.dto;

import bo.phishshield.backend.qa.domain.model.ResumenEjecucion;

import java.math.BigDecimal;

/** Conteo de casos por estado; terminados sirve para la barra de avance ("3 de 8"). */
public record ResumenResponse(int total, int pass, int fail, int skipped, int notRun,
                              int terminados, BigDecimal porcentajeExito) {

    public static ResumenResponse desde(ResumenEjecucion r) {
        return new ResumenResponse(r.total(), r.pass(), r.fail(), r.skipped(), r.notRun(),
                r.terminados(), r.porcentajeExito());
    }
}
