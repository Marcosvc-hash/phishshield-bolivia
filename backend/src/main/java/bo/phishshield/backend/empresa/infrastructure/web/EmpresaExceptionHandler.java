package bo.phishshield.backend.empresa.infrastructure.web;

import bo.phishshield.backend.empresa.domain.model.EmpresaNoEncontradaException;
import bo.phishshield.backend.empresa.domain.model.EmpresaYaExisteException;
import bo.phishshield.backend.shared.infrastructure.web.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce excepciones propias de la vertical empresa a respuestas HTTP.
 * El @Order evita que el manejador generico de Exception las atrape antes.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EmpresaExceptionHandler {

    @ExceptionHandler(EmpresaYaExisteException.class)
    public ResponseEntity<ErrorResponse> empresaYaExiste(
            EmpresaYaExisteException ex, HttpServletRequest peticion) {

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponse.simple(
                        HttpStatus.CONFLICT.value(),
                        HttpStatus.CONFLICT.getReasonPhrase(),
                        ex.getMessage(),
                        peticion.getRequestURI()));
    }

    @ExceptionHandler(EmpresaNoEncontradaException.class)
    public ResponseEntity<ErrorResponse> empresaNoEncontrada(
            EmpresaNoEncontradaException ex, HttpServletRequest peticion) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ErrorResponse.simple(
                        HttpStatus.NOT_FOUND.value(),
                        HttpStatus.NOT_FOUND.getReasonPhrase(),
                        ex.getMessage(),
                        peticion.getRequestURI()));
    }
}
