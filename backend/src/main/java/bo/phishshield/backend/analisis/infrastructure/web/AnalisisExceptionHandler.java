package bo.phishshield.backend.analisis.infrastructure.web;

import bo.phishshield.backend.analisis.domain.model.UrlInvalidaException;
import bo.phishshield.backend.shared.infrastructure.web.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce excepciones propias de la vertical analisis a respuestas HTTP.
 *
 * El @Order es necesario: GlobalExceptionHandler tiene un manejador
 * para Exception.class que atraparia esto primero y devolveria un 500.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AnalisisExceptionHandler {

    @ExceptionHandler(UrlInvalidaException.class)
    public ResponseEntity<ErrorResponse> urlInvalida(
            UrlInvalidaException ex, HttpServletRequest peticion) {

        return ResponseEntity.badRequest().body(
                ErrorResponse.simple(
                        HttpStatus.BAD_REQUEST.value(),
                        "URL invalida",
                        ex.getMessage(),
                        peticion.getRequestURI()));
    }
}