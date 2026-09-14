package bo.phishshield.backend.shared.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        OffsetDateTime momento,
        int estado,
        String error,
        String mensaje,
        String ruta,
        List<CampoInvalido> errores
) {

    public record CampoInvalido(String campo, String mensaje) {}

    public static ErrorResponse simple(int estado, String error, String mensaje, String ruta) {
        return new ErrorResponse(OffsetDateTime.now(), estado, error, mensaje, ruta, null);
    }

    public static ErrorResponse conCampos(int estado, String error, String mensaje,
                                          String ruta, List<CampoInvalido> errores) {
        return new ErrorResponse(OffsetDateTime.now(), estado, error, mensaje, ruta, errores);
    }
}