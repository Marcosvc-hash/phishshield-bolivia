package bo.phishshield.backend.reporte.infrastructure.web;

import bo.phishshield.backend.reporte.domain.model.PeriodoInvalidoException;
import bo.phishshield.backend.reporte.domain.model.ReporteNoEncontradoException;
import bo.phishshield.backend.shared.infrastructure.web.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce las excepciones de reportes a respuestas HTTP (404 y 400). */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ReporteExceptionHandler {

    @ExceptionHandler(ReporteNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(RuntimeException ex, HttpServletRequest peticion) {
        return respuesta(HttpStatus.NOT_FOUND, ex, peticion);
    }

    @ExceptionHandler(PeriodoInvalidoException.class)
    public ResponseEntity<ErrorResponse> periodoInvalido(RuntimeException ex, HttpServletRequest peticion) {
        return respuesta(HttpStatus.BAD_REQUEST, ex, peticion);
    }

    private static ResponseEntity<ErrorResponse> respuesta(HttpStatus estado, RuntimeException ex,
                                                           HttpServletRequest peticion) {
        return ResponseEntity.status(estado).body(ErrorResponse.simple(estado.value(), estado.getReasonPhrase(),
                ex.getMessage(), peticion.getRequestURI()));
    }
}
