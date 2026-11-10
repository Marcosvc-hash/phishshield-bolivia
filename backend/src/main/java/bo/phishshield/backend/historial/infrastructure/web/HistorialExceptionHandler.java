package bo.phishshield.backend.historial.infrastructure.web;

import bo.phishshield.backend.historial.domain.model.AnalisisNoEncontradoException;
import bo.phishshield.backend.shared.infrastructure.web.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones del historial a respuestas HTTP.
 * El @Order evita que el manejador generico de Exception las atrape antes.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HistorialExceptionHandler {

    @ExceptionHandler(AnalisisNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(
            AnalisisNoEncontradoException ex, HttpServletRequest peticion) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ErrorResponse.simple(
                        HttpStatus.NOT_FOUND.value(),
                        HttpStatus.NOT_FOUND.getReasonPhrase(),
                        ex.getMessage(),
                        peticion.getRequestURI()));
    }
}
