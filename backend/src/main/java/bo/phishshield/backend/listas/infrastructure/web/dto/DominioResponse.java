package bo.phishshield.backend.listas.infrastructure.web.dto;

import bo.phishshield.backend.listas.domain.model.DominioFila;

import java.time.OffsetDateTime;
import java.util.UUID;

import static bo.phishshield.backend.shared.infrastructure.web.dto.FechasLocales.enZonaLocal;

/** Una fila de las listas. "id" es dominios_lista.uuid_publico (V9). */
public record DominioResponse(
        UUID id,
        String dominio,
        String lista,
        String entidad,
        String fuente,
        String motivo,
        boolean activo,
        OffsetDateTime creadoEn
) {

    public static DominioResponse desde(DominioFila d) {
        return new DominioResponse(d.id(), d.dominio(), d.lista(), d.entidad(), d.fuente(), d.motivo(),
                d.activo(), enZonaLocal(d.creadoEn()));
    }
}
