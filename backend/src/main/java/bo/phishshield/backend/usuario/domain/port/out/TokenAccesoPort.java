package bo.phishshield.backend.usuario.domain.port.out;

import bo.phishshield.backend.usuario.domain.model.TokenAcceso;
import bo.phishshield.backend.usuario.domain.model.Usuario;

/** Emite el token de acceso de corta duracion (en infraestructura, un JWT firmado). */
public interface TokenAccesoPort {

    TokenAcceso emitir(Usuario usuario);
}
