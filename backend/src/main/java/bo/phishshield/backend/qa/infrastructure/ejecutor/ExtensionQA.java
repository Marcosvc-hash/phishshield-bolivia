package bo.phishshield.backend.qa.infrastructure.ejecutor;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.util.UUID;

/**
 * Extension de JUnit que entrega un ContextoCaso a cada metodo de prueba.
 *
 * El ejecutor pasa el id de la ejecucion como parametro de configuracion de
 * JUnit; con el, la extension busca la sesion en el registro. Un
 * ParameterResolver es lo mas parecido a la inyeccion de dependencias por
 * parametro de un metodo de accion en ASP.NET.
 */
public class ExtensionQA implements ParameterResolver {

    /** Nombre del parametro de configuracion de JUnit con el id de la ejecucion. */
    public static final String PARAMETRO_EJECUCION = "phishshield.qa.ejecucion";

    @Override
    public boolean supportsParameter(ParameterContext parametro, ExtensionContext contexto) {
        return parametro.getParameter().getType() == ContextoCaso.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parametro, ExtensionContext contexto) {
        String id = contexto.getConfigurationParameter(PARAMETRO_EJECUCION)
                .orElseThrow(() -> new ParameterResolutionException(
                        "Este caso solo se ejecuta desde el Centro de Pruebas (falta " + PARAMETRO_EJECUCION + ")"));
        SesionEjecucion sesion = SesionEjecucion.Registro.buscar(UUID.fromString(id));
        if (sesion == null || sesion.casoActual() == null) {
            throw new ParameterResolutionException("No hay una ejecución activa con id " + id);
        }
        return new ContextoCaso(sesion, sesion.casoActual());
    }
}
