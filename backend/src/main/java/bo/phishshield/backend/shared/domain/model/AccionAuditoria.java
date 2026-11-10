package bo.phishshield.backend.shared.domain.model;

/**
 * Acciones administrativas que quedan en la tabla auditoria (columna accion).
 * Los accesos (LOGIN_EXITOSO, LOGIN_FALLIDO, LOGOUT) siguen en
 * usuario.domain.model.TipoEventoAcceso; las dos listas comparten la tabla.
 */
public enum AccionAuditoria {

    USUARIO_CREADO,
    USUARIO_ACTIVADO,
    USUARIO_DESACTIVADO,
    USUARIO_ROL_CAMBIADO,

    ALERTA_ACTUALIZADA,

    DOMINIO_AGREGADO,
    DOMINIO_EDITADO,
    DOMINIO_DESACTIVADO,
    DOMINIO_REACTIVADO,

    EMPRESA_CREADA,
    EMPRESA_EDITADA,
    EMPRESA_ACTIVADA,
    EMPRESA_DESACTIVADA;

    /**
     * Los cambios en las listas blanca y negra hacen que el motor deje de
     * reutilizar los analisis guardados antes del cambio (ver AnalizarUrlUseCase).
     */
    public boolean cambiaListasDeDominios() {
        return name().startsWith("DOMINIO_");
    }
}
