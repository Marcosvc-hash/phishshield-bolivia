package bo.phishshield.backend.qa.infrastructure.web;

import bo.phishshield.backend.qa.domain.model.AsistenteNoDisponibleException;
import bo.phishshield.backend.qa.domain.model.CorreoInvalidoException;
import bo.phishshield.backend.qa.domain.model.EjecucionEnCursoException;
import bo.phishshield.backend.qa.domain.model.EjecucionNoEncontradaException;
import bo.phishshield.backend.qa.domain.model.EvidenciaNoEncontradaException;
import bo.phishshield.backend.qa.domain.model.ReporteNoDisponibleException;
import bo.phishshield.backend.qa.domain.model.SeleccionInvalidaException;
import bo.phishshield.backend.qa.domain.model.TransicionEjecucionInvalidaException;
import bo.phishshield.backend.shared.infrastructure.web.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones del Centro de Pruebas a respuestas HTTP.
 * El @Order evita que el manejador generico de Exception las atrape antes.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class QaExceptionHandler {

    @ExceptionHandler({EjecucionNoEncontradaException.class, EvidenciaNoEncontradaException.class})
    public ResponseEntity<ErrorResponse> noEncontrada(RuntimeException ex, HttpServletRequest peticion) {
        return respuesta(HttpStatus.NOT_FOUND, ex, peticion);
    }

    @ExceptionHandler({SeleccionInvalidaException.class, CorreoInvalidoException.class})
    public ResponseEntity<ErrorResponse> peticionInvalida(RuntimeException ex, HttpServletRequest peticion) {
        return respuesta(HttpStatus.BAD_REQUEST, ex, peticion);
    }

    /** 409: ya hay una ejecucion activa, la ejecucion ya termino, o todavia no tiene reportes. */
    @ExceptionHandler({EjecucionEnCursoException.class, TransicionEjecucionInvalidaException.class,
            ReporteNoDisponibleException.class})
    public ResponseEntity<ErrorResponse> conflicto(RuntimeException ex, HttpServletRequest peticion) {
        return respuesta(HttpStatus.CONFLICT, ex, peticion);
    }

    /** 503: el asistente de IA no esta configurado o no respondio (las pruebas funcionan igual). */
    @ExceptionHandler(AsistenteNoDisponibleException.class)
    public ResponseEntity<ErrorResponse> asistenteNoDisponible(RuntimeException ex, HttpServletRequest peticion) {
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, ex, peticion);
    }

    private static ResponseEntity<ErrorResponse> respuesta(HttpStatus estado, RuntimeException ex,
                                                           HttpServletRequest peticion) {
        return ResponseEntity.status(estado).body(
                ErrorResponse.simple(estado.value(), estado.getReasonPhrase(), ex.getMessage(),
                        peticion.getRequestURI()));
    }
}
