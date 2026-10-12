package bo.phishshield.backend.empresa.domain.model;

/**
 * Regla de negocio: no puede haber dos empresas con el mismo NIT.
 * La capa web la traduce a 409 Conflict.
 */
public class EmpresaYaExisteException extends RuntimeException {

    public EmpresaYaExisteException(String nit) {
        super("Ya existe una empresa registrada con el NIT " + nit);
    }
}
