package bo.phishshield.backend.usuario.domain.model;

/** Se guarda tal cual en auditoria.accion (el seed ya usa LOGIN_EXITOSO). */
public enum TipoEventoAcceso {

    LOGIN_EXITOSO,
    LOGIN_FALLIDO,
    LOGOUT
}
