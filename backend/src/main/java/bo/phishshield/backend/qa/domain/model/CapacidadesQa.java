package bo.phishshield.backend.qa.domain.model;

/**
 * Que funciones opcionales estan activas en este backend: el envio de
 * reportes por correo (MAIL_*) y el asistente de IA (clave de un LLM).
 * La pantalla oculta lo que no esta disponible.
 */
public record CapacidadesQa(boolean correoConfigurado, boolean asistenteDisponible) {
}
