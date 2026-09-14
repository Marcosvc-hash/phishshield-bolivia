package bo.phishshield.backend.analisis.domain.service;

import bo.phishshield.backend.analisis.domain.model.UrlAnalizada;

import java.util.Optional;

/**
 * Una heuristica explicable sobre una URL.
 * Cada implementacion responde una sola pregunta y no conoce su propio peso:
 * el peso vive en la tabla reglas_deteccion y lo aplica el motor.
 */
public interface ReglaUrl {

    /** Debe coincidir exactamente con reglas_deteccion.codigo */
    String codigo();

    /**
     * Devuelve la evidencia encontrada, o vacio si la regla no se disparo.
     * El texto devuelto se guarda en analisis_indicadores.valor y es
     * lo que se le muestra al usuario para explicarle el veredicto.
     */
    Optional<String> evaluar(UrlAnalizada url);
}