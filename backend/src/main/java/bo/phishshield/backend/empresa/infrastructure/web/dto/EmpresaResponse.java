package bo.phishshield.backend.empresa.infrastructure.web.dto;

import bo.phishshield.backend.empresa.domain.model.Empresa;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EmpresaResponse(
        UUID id,
        String nombre,
        String nit,
        String sector,
        String ciudad,
        String plan,
        boolean activa,
        OffsetDateTime creadoEn
) {
    public static EmpresaResponse desde(Empresa empresa) {
        return new EmpresaResponse(
                empresa.getUuidPublico(),
                empresa.getNombre(),
                empresa.getNit(),
                empresa.getSector(),
                empresa.getCiudad(),
                empresa.getPlan(),
                empresa.isActiva(),
                empresa.getCreadoEn()
        );
    }
}