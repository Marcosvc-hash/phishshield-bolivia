package bo.phishshield.backend.alerta.infrastructure.web;

import bo.phishshield.backend.alerta.domain.model.AlertaModificadaException;
import bo.phishshield.backend.alerta.domain.model.AlertaNoEncontradaException;
import bo.phishshield.backend.alerta.domain.model.TransicionAlertaInvalidaException;
import bo.phishshield.backend.shared.infrastructure.web.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones de alertas a respuestas HTTP.
 * El @Order evita que el manejador generico de Exception las atrape antes.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AlertaExceptionHandler {

    @ExceptionHandler(AlertaNoEncontradaException.class)
    public ResponseEntity<ErrorResponse> noEncontrada(RuntimeException ex, HttpServletRequest peticion) {
        return respuesta(HttpStatus.NOT_FOUND, ex, peticion);
    }

    /** 409 Conflict: la peticion es correcta, pero choca con el estado actual de la alerta. */
    @ExceptionHandler({TransicionAlertaInvalidaException.class, AlertaModificadaException.class})
    public ResponseEntity<ErrorResponse> conflicto(RuntimeException ex, HttpServletRequest peticion) {
        return respuesta(HttpStatus.CONFLICT, ex, peticion);
    }

    private static ResponseEntity<ErrorResponse> respuesta(HttpStatus estado, RuntimeException ex,
                                                           HttpServletRequest peticion) {
        return ResponseEntity.status(estado).body(
                ErrorResponse.simple(
                        estado.value(),
                        estado.getReasonPhrase(),
                        ex.getMessage(),
                        peticion.getRequestURI()));
    }
}
