package bo.phishshield.backend.usuario.domain.model;

/**
 * Un intento de acceso, exitoso o no. idUsuario es null cuando alguien
 * intenta entrar con un email que no esta registrado; en ese caso el email
 * queda como evidencia en el detalle.
 *
 * @param motivo por que fallo (contrasena_incorrecta, email_no_registrado,
 *               cuenta_inactiva...); null si el acceso fue exitoso
 */
public record EventoAcceso(TipoEventoAcceso tipo,
                           Long idUsuario,
                           String email,
                           String motivo,
                           String ip,
                           String userAgent) {

    public EventoAcceso {
        if (tipo == null) {
            throw new IllegalArgumentException("El evento de acceso necesita un tipo");
        }
    }
}
