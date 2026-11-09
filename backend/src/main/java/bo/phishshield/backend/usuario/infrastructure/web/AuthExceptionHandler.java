package bo.phishshield.backend.usuario.infrastructure.web;

import bo.phishshield.backend.shared.infrastructure.web.dto.ErrorResponse;
import bo.phishshield.backend.usuario.domain.model.CredencialesInvalidasException;
import bo.phishshield.backend.usuario.domain.model.CuentaNoHabilitadaException;
import bo.phishshield.backend.usuario.domain.model.EmailYaRegistradoException;
import bo.phishshield.backend.usuario.domain.model.SesionInvalidaException;
import bo.phishshield.backend.usuario.domain.model.UsuarioNoEncontradoException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones de autenticacion a respuestas HTTP.
 * El @Order evita que el manejador generico de Exception las atrape antes.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthExceptionHandler {

    @ExceptionHandler({CredencialesInvalidasException.class, SesionInvalidaException.class})
    public ResponseEntity<ErrorResponse> noAutenticado(RuntimeException ex, HttpServletRequest peticion) {
        return respuesta(HttpStatus.UNAUTHORIZED, ex, peticion);
    }

    @ExceptionHandler(CuentaNoHabilitadaException.class)
    public ResponseEntity<ErrorResponse> cuentaNoHabilitada(RuntimeException ex, HttpServletRequest peticion) {
        return respuesta(HttpStatus.FORBIDDEN, ex, peticion);
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> usuarioNoEncontrado(RuntimeException ex, HttpServletRequest peticion) {
        return respuesta(HttpStatus.NOT_FOUND, ex, peticion);
    }

    @ExceptionHandler(EmailYaRegistradoException.class)
    public ResponseEntity<ErrorResponse> emailYaRegistrado(RuntimeException ex, HttpServletRequest peticion) {
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
