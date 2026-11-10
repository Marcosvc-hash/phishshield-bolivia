package bo.phishshield.backend.usuario.domain.model;

import java.time.OffsetDateTime;

/**
 * Una fila de la tabla sesiones. Solo se guarda el hash del token de
 * refresco: si alguien lee la base, no puede usar los tokens.
 */
public record SesionRefresco(Long id,
                             Long idUsuario,
                             String hashToken,
                             OffsetDateTime expiraEn,
                             boolean revocada,
                             String userAgent) {

    public SesionRefresco {
        if (idUsuario == null || hashToken == null || expiraEn == null) {
            throw new IllegalArgumentException("La sesion necesita usuario, hash y vencimiento");
        }
    }

    public static SesionRefresco nueva(Long idUsuario, String hashToken,
                                       OffsetDateTime expiraEn, String userAgent) {
        return new SesionRefresco(null, idUsuario, hashToken, expiraEn, false, userAgent);
    }

    public boolean vigente(OffsetDateTime ahora) {
        return !revocada && expiraEn.isAfter(ahora);
    }
}
