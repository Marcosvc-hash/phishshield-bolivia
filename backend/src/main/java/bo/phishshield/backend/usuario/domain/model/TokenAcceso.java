package bo.phishshield.backend.usuario.domain.model;

import java.time.OffsetDateTime;

/** El token de acceso ya firmado (el dominio no sabe que es un JWT). */
public record TokenAcceso(String valor, OffsetDateTime expiraEn) {

    public TokenAcceso {
        if (valor == null || valor.isBlank() || expiraEn == null) {
            throw new IllegalArgumentException("El token de acceso necesita valor y vencimiento");
        }
    }
}
