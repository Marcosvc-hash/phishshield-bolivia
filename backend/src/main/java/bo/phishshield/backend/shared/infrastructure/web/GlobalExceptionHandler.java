package bo.phishshield.backend.shared.infrastructure.web;

import bo.phishshield.backend.shared.infrastructure.web.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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

    /**
     * Violacion de una restriccion de la base (FK, UNIQUE, CHECK).
     * El texto de PostgreSQL revela tablas y columnas: va al log, no al cliente.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> conflictoDeDatos(
            DataIntegrityViolationException ex, HttpServletRequest peticion) {

        log.warn("Restriccion de la base violada en {}: {}",
                peticion.getRequestURI(), ex.getMostSpecificCause().getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponse.simple(
                        HttpStatus.CONFLICT.value(),
                        "Conflicto de datos",
                        "La operacion contradice datos existentes; revise que las referencias "
                                + "(usuario, empresa) existan y que no haya duplicados.",
                        peticion.getRequestURI()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> cuerpoIlegible(
            HttpMessageNotReadableException ex, HttpServletRequest peticion) {

        return ResponseEntity.badRequest().body(
                ErrorResponse.simple(
                        HttpStatus.BAD_REQUEST.value(),
                        "Solicitud invalida",
                        "El cuerpo de la peticion no es un JSON valido",
                        peticion.getRequestURI()));
    }

    /** Por ejemplo, GET /api/analisis/abc cuando se espera un UUID. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> parametroConFormatoInvalido(
            MethodArgumentTypeMismatchException ex, HttpServletRequest peticion) {

        return ResponseEntity.badRequest().body(
                ErrorResponse.simple(
                        HttpStatus.BAD_REQUEST.value(),
                        "Solicitud invalida",
                        "El parametro '" + ex.getName() + "' tiene un formato invalido",
                        peticion.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> errorInesperado(
            Exception ex, HttpServletRequest peticion) {

        // Excepciones de Spring MVC que ya traen su estado HTTP (ruta inexistente,
        // metodo no permitido, parametro faltante): no son errores del servidor.
        // Nombre completo porque nuestro DTO tambien se llama ErrorResponse.
        if (ex instanceof org.springframework.web.ErrorResponse conEstado) {
            int codigo = conEstado.getStatusCode().value();
            HttpStatus estado = HttpStatus.resolve(codigo);
            return ResponseEntity.status(codigo).body(
                    ErrorResponse.simple(
                            codigo,
                            estado != null ? estado.getReasonPhrase() : "Error",
                            conEstado.getBody().getDetail(),
                            peticion.getRequestURI()));
        }

        log.error("Error no controlado en {}", peticion.getRequestURI(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponse.simple(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Error interno",
                        "Ocurrio un error inesperado. Revise el registro del servidor.",
                        peticion.getRequestURI()));
    }
}