package bo.phishshield.backend.qa.infrastructure.web.dto;

import bo.phishshield.backend.qa.domain.model.FilaEjecucion;

import java.time.OffsetDateTime;
import java.util.UUID;

import static bo.phishshield.backend.shared.infrastructure.web.dto.FechasLocales.enZonaLocal;

/** Una fila del historial de ejecuciones. "id" es qa_ejecuciones.uuid_publico. */
public record FilaEjecucionResponse(
        UUID id,
        String tipo,
        String suite,
        String suiteEtiqueta,
        String alcance,
        String correo,
        String estado,
        String resultadoGlobal,
        OffsetDateTime creadaEn,
        OffsetDateTime inicio,
        OffsetDateTime fin,
        Long duracionMs,
        ResumenResponse resumen,
        String estadoCorreo
) {

    public static FilaEjecucionResponse desde(FilaEjecucion f) {
        return new FilaEjecucionResponse(
                f.id(),
                f.tipo().aValorBd(),
                f.suite().aValorBd(), f.suite().etiqueta(),
                f.alcance().aValorBd(),
                f.correo(),
                f.estado().aValorBd(),
                f.resultadoGlobal() == null ? null : f.resultadoGlobal().aValorBd(),
                enZonaLocal(f.creadaEn()), enZonaLocal(f.inicio()), enZonaLocal(f.fin()),
                f.duracionMs(),
                ResumenResponse.desde(f.resumen()),
                f.estadoCorreo().aValorBd());
    }
}
