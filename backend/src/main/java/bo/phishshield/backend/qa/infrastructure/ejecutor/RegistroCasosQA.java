package bo.phishshield.backend.qa.infrastructure.ejecutor;

import bo.phishshield.backend.qa.infrastructure.ejecutor.casos.api.CasosIntegrationApi;
import bo.phishshield.backend.qa.infrastructure.ejecutor.casos.api.CasosRegressionApi;
import bo.phishshield.backend.qa.infrastructure.ejecutor.casos.api.CasosSanityApi;
import bo.phishshield.backend.qa.infrastructure.ejecutor.casos.api.CasosSmokeApi;
import bo.phishshield.backend.qa.infrastructure.ejecutor.casos.carga.CasosCarga;
import bo.phishshield.backend.qa.infrastructure.ejecutor.casos.carga.CasosRobustez;
import bo.phishshield.backend.qa.infrastructure.ejecutor.casos.ui.CasosE2eUi;
import bo.phishshield.backend.qa.infrastructure.ejecutor.casos.ui.CasosRegressionUi;
import bo.phishshield.backend.qa.infrastructure.ejecutor.casos.ui.CasosSmokeUi;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Las clases con casos programados y el metodo de cada codigo @CasoQA.
 *
 * La lista de clases es explicita a proposito: escanear el classpath dentro
 * del jar de Spring Boot es fragil, y asi se ve de un vistazo que hay.
 */
public final class RegistroCasosQA {

    /** Clases con metodos @Test @CasoQA. Al agregar una clase de casos, se suma aqui. */
    static final List<Class<?>> CLASES = List.of(
            CasosSmokeApi.class,
            CasosSanityApi.class,
            CasosRegressionApi.class,
            CasosIntegrationApi.class,
            CasosSmokeUi.class,
            CasosRegressionUi.class,
            CasosE2eUi.class,
            CasosCarga.class,
            CasosRobustez.class);

    private final Map<String, Method> metodos;

    public RegistroCasosQA() {
        this(CLASES);
    }

    RegistroCasosQA(List<Class<?>> clases) {
        Map<String, Method> encontrados = new TreeMap<>();
        for (Class<?> clase : clases) {
            for (Method metodo : clase.getDeclaredMethods()) {
                CasoQA caso = metodo.getAnnotation(CasoQA.class);
                if (caso == null) {
                    continue;
                }
                if (!metodo.isAnnotationPresent(Test.class)) {
                    throw new IllegalStateException(clase.getSimpleName() + "." + metodo.getName()
                            + " tiene @CasoQA pero no @Test");
                }
                Method anterior = encontrados.put(caso.value(), metodo);
                if (anterior != null) {
                    throw new IllegalStateException("El caso " + caso.value() + " está programado dos veces: "
                            + anterior.getDeclaringClass().getSimpleName() + "." + anterior.getName() + " y "
                            + clase.getSimpleName() + "." + metodo.getName());
                }
            }
        }
        this.metodos = Collections.unmodifiableMap(encontrados);
    }

    public Optional<Method> metodo(String codigo) {
        return Optional.ofNullable(metodos.get(codigo));
    }

    public Set<String> codigos() {
        return metodos.keySet();
    }
}
