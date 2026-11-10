package bo.phishshield.backend.listas.infrastructure.web;

import bo.phishshield.backend.listas.domain.model.ConflictoListaException;
import bo.phishshield.backend.listas.domain.model.DominioNoEncontradoException;
import bo.phishshield.backend.shared.infrastructure.web.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones de listas a respuestas HTTP.
 * El @Order evita que el manejador generico de Exception las atrape antes.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ListasExceptionHandler {

    @ExceptionHandler(DominioNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(RuntimeException ex, HttpServletRequest peticion) {
        return respuesta(HttpStatus.NOT_FOUND, ex, peticion);
    }

    @ExceptionHandler(ConflictoListaException.class)
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
