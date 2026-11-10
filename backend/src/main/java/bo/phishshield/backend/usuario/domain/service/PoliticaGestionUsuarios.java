package bo.phishshield.backend.usuario.domain.service;

import bo.phishshield.backend.shared.domain.model.AccesoDenegadoException;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.Rol;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import bo.phishshield.backend.usuario.domain.model.Usuario;

/**
 * Quien puede gestionar a quien. Logica pura, sin Spring:
 *   - Nadie cambia el estado ni el rol de su propia cuenta (un admin no
 *     puede dejarse afuera por error, ni dejar la plataforma sin administradores).
 *   - El ADMIN gestiona a cualquiera.
 *   - El ADMIN_EMPRESA solo a usuarios de su empresa, y nunca a un ADMIN.
 *     Tampoco puede crear un ADMIN ni ascender a nadie a ADMIN.
 *   - El USUARIO no gestiona a nadie.
 */
public final class PoliticaGestionUsuarios {

    private PoliticaGestionUsuarios() {
        // clase de utilidad: no se instancia
    }

    public static void validarQuePuedeGestionar(UsuarioActual quien, Usuario objetivo) {
        AlcanceDatos alcance = AlcanceDatos.administrativo(quien);

        if (objetivo.getId().equals(quien.idUsuario())) {
            throw new AccesoDenegadoException("No puede cambiar el estado ni el rol de su propia cuenta");
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

    /**
     * Decide a que empresa va un usuario nuevo y si quien lo crea puede
     * darle ese rol. Devuelve el id de la empresa (null = sin empresa).
     *
     * @param idEmpresaPedida la empresa que vino en la peticion (null = ninguna)
     */
    public static Long empresaParaAlta(UsuarioActual quien, Rol rolNuevo, Long idEmpresaPedida) {
        AlcanceDatos alcance = AlcanceDatos.administrativo(quien);
        if (rolNuevo == null) {
            throw new IllegalArgumentException("Indique el rol del usuario nuevo");
        }

        if (!alcance.esGlobal()) {
            if (rolNuevo == Rol.ADMIN) {
                throw new AccesoDenegadoException("Un administrador de empresa no puede crear administradores de la plataforma");
            }
            if (idEmpresaPedida != null && !idEmpresaPedida.equals(alcance.idEmpresa())) {
                throw new AccesoDenegadoException("Solo puede crear usuarios de su propia empresa");
            }
            return alcance.idEmpresa();
        }

        if (rolNuevo == Rol.ADMIN && idEmpresaPedida != null) {
            throw new IllegalArgumentException("Un administrador de la plataforma no pertenece a una empresa");
        }
        if (rolNuevo == Rol.ADMIN_EMPRESA && idEmpresaPedida == null) {
            throw new IllegalArgumentException("Un administrador de empresa necesita una empresa");
        }
        return idEmpresaPedida;
    }

    /** Las empresas desactivadas no reciben usuarios nuevos. */
    public static void validarEmpresaActiva(boolean activa, String nombreEmpresa) {
        if (!activa) {
            throw new IllegalArgumentException("La empresa " + nombreEmpresa
                    + " esta desactivada: no se le pueden crear usuarios");
        }
    }

    /** Ademas de poder gestionar al usuario, el rol nuevo tiene que estar a su alcance. */
    public static void validarCambioDeRol(UsuarioActual quien, Usuario objetivo, Rol nuevo) {
        validarQuePuedeGestionar(quien, objetivo);
        if (nuevo == null) {
            throw new IllegalArgumentException("Indique el rol nuevo");
        }
        if (nuevo == Rol.ADMIN && quien.rol() != Rol.ADMIN) {
            throw new AccesoDenegadoException("Solo un administrador de la plataforma puede asignar ese rol");
        }
        if (nuevo == Rol.ADMIN_EMPRESA && objetivo.getIdEmpresa() == null) {
            throw new IllegalArgumentException(
                    "Para ser administrador de empresa, el usuario debe pertenecer a una empresa");
        }
    }
}
