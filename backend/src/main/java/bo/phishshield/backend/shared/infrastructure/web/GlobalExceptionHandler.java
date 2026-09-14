package bo.phishshield.backend.shared.infrastructure.web;

import bo.phishshield.backend.shared.infrastructure.web.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacionFallida(
            MethodArgumentNotValidException ex, HttpServletRequest peticion) {

        List<ErrorResponse.CampoInvalido> campos = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> new ErrorResponse.CampoInvalido(e.getField(), e.getDefaultMessage()))
                .toList();

        return ResponseEntity.badRequest().body(
                ErrorResponse.conCampos(
                        HttpStatus.BAD_REQUEST.value(),
                        "Solicitud invalida",
                        "Hay " + campos.size() + " campo(s) con errores de validacion",
                        peticion.getRequestURI(),
                        campos));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> reglaDeDominioViolada(
            IllegalArgumentException ex, HttpServletRequest peticion) {

        return ResponseEntity.badRequest().body(
                ErrorResponse.simple(
                        HttpStatus.BAD_REQUEST.value(),
                        "Regla de negocio no cumplida",
                        ex.getMessage(),
                        peticion.getRequestURI()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> estadoExplicito(
            ResponseStatusException ex, HttpServletRequest peticion) {

        HttpStatus estado = HttpStatus.valueOf(ex.getStatusCode().value());

        return ResponseEntity.status(estado).body(
                ErrorResponse.simple(
                        estado.value(),
                        estado.getReasonPhrase(),
                        ex.getReason(),
                        peticion.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> errorInesperado(
            Exception ex, HttpServletRequest peticion) {

        log.error("Error no controlado en {}", peticion.getRequestURI(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponse.simple(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Error interno",
                        "Ocurrio un error inesperado. Revise el registro del servidor.",
                        peticion.getRequestURI()));
    }
}