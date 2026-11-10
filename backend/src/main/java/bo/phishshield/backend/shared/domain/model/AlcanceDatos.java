package bo.phishshield.backend.shared.domain.model;

/**
 * Que datos puede ver quien hace la peticion:
 *   ADMIN          -> toda la plataforma (idEmpresa = null)
 *   ADMIN_EMPRESA  -> solo su empresa
 *   USUARIO        -> no tiene acceso a datos administrativos
 *
 * Los adaptadores filtran con esto; asi la regla vive en un solo lugar
 * y no repetida en cada consulta SQL.
 */
public record AlcanceDatos(Long idEmpresa) {

    public static AlcanceDatos administrativo(UsuarioActual quien) {
        return switch (quien.rol()) {
            case ADMIN -> new AlcanceDatos(null);
            case ADMIN_EMPRESA -> {
                if (quien.idEmpresa() == null) {
                    throw new AccesoDenegadoException(
                            "La cuenta de administrador de empresa no tiene una empresa asignada");
                }
                yield new AlcanceDatos(quien.idEmpresa());
            }
            case USUARIO -> throw new AccesoDenegadoException(
                    "Los datos administrativos son solo para administradores");
        };
    }

    /**
     * Solo el ADMIN: listas de dominios y registro de accesos son datos de
     * toda la plataforma, no de una empresa.
     */
    public static AlcanceDatos soloPlataforma(UsuarioActual quien) {
        if (quien.rol() != Rol.ADMIN) {
            throw new AccesoDenegadoException("Esta seccion es solo para el administrador de la plataforma");
        }
        return new AlcanceDatos(null);
    }

    public boolean esGlobal() {
        return idEmpresa == null;
    }

    /**
     * Si un dato de esa empresa entra en el alcance. Un dato sin empresa
     * (por ejemplo, un analisis anonimo) solo lo ve el alcance global.
     */
    public boolean incluye(Long idEmpresaDelDato) {
        return esGlobal() || idEmpresa.equals(idEmpresaDelDato);
    }
}
