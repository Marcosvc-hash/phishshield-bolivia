package bo.phishshield.backend.shared.infrastructure.security;

import bo.phishshield.backend.shared.infrastructure.web.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

/**
 * Respuestas 401 y 403 con el mismo formato JSON (ErrorResponse) que el resto
 * de la API. Sin esto, Spring Security responde con un cuerpo vacio.
 *
 * Estos errores ocurren en los filtros, antes de llegar a un controlador,
 * por eso no los atrapan los @RestControllerAdvice.
 */
@Component
public class RespuestasSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final JsonMapper json;

    public RespuestasSeguridad(JsonMapper json) {
        this.json = json;
    }

    /** 401: no mando token, o el token es invalido o vencio. */
    @Override
    public void commence(HttpServletRequest peticion, HttpServletResponse respuesta,
                         AuthenticationException ex) throws IOException {
        respuesta.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        escribir(respuesta, HttpStatus.UNAUTHORIZED,
                "Necesita iniciar sesion, o su sesion vencio", peticion.getRequestURI());
    }

    /** 403: el token es valido, pero su rol no tiene permiso para esta ruta. */
    @Override
    public void handle(HttpServletRequest peticion, HttpServletResponse respuesta,
                       AccessDeniedException ex) throws IOException {
        escribir(respuesta, HttpStatus.FORBIDDEN,
                "Su rol no tiene permiso para esta operacion", peticion.getRequestURI());
    }

    private void escribir(HttpServletResponse respuesta, HttpStatus estado,
                          String mensaje, String ruta) throws IOException {
        respuesta.setStatus(estado.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding("UTF-8");
        json.writeValue(respuesta.getOutputStream(),
                ErrorResponse.simple(estado.value(), estado.getReasonPhrase(), mensaje, ruta));
    }
}
