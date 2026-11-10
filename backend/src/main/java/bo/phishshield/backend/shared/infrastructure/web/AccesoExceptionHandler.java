package bo.phishshield.backend.shared.infrastructure.web;

import bo.phishshield.backend.shared.domain.model.AccesoDenegadoException;
import bo.phishshield.backend.shared.infrastructure.web.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 403 cuando un caso de uso rechaza al usuario por su rol. Es la segunda
 * barrera: la primera son las reglas por ruta de SeguridadConfig.
 * El @Order evita que el manejador generico de Exception la atrape antes.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccesoExceptionHandler {

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<ErrorResponse> accesoDenegado(
            AccesoDenegadoException ex, HttpServletRequest peticion) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ErrorResponse.simple(
                        HttpStatus.FORBIDDEN.value(),
                        HttpStatus.FORBIDDEN.getReasonPhrase(),
                        ex.getMessage(),
                        peticion.getRequestURI()));
    }
}
