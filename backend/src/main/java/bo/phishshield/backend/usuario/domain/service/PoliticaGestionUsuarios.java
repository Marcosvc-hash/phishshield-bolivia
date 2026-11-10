package bo.phishshield.backend.usuario.domain.service;

import bo.phishshield.backend.shared.domain.model.AccesoDenegadoException;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.Rol;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import bo.phishshield.backend.usuario.domain.model.Usuario;

/**
 * Quien puede gestionar a quien. Logica pura, sin Spring:
 *   - Nadie cambia el estado de su propia cuenta (un admin no puede dejarse
 *     afuera por error, ni dejar la plataforma sin administradores).
 *   - El ADMIN gestiona a cualquiera.
 *   - El ADMIN_EMPRESA solo a usuarios de su empresa, y nunca a un ADMIN.
 *   - El USUARIO no gestiona a nadie.
 */
public final class PoliticaGestionUsuarios {

    private PoliticaGestionUsuarios() {
        // clase de utilidad: no se instancia
    }

    public static void validarQuePuedeGestionar(UsuarioActual quien, Usuario objetivo) {
        AlcanceDatos alcance = AlcanceDatos.administrativo(quien);

        if (objetivo.getId().equals(quien.idUsuario())) {
            throw new AccesoDenegadoException("No puede cambiar el estado de su propia cuenta");
        }
        if (alcance.esGlobal()) {
            return;
        }
        if (!alcance.idEmpresa().equals(objetivo.getIdEmpresa())) {
            throw new AccesoDenegadoException("Solo puede gestionar usuarios de su empresa");
        }
        if (objetivo.getRol() == Rol.ADMIN) {
            throw new AccesoDenegadoException("No puede gestionar a un administrador de la plataforma");
        }
    }
}
