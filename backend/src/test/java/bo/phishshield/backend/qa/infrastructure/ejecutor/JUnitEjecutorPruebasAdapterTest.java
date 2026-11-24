package bo.phishshield.backend.qa.infrastructure.ejecutor;

import bo.phishshield.backend.qa.domain.model.DesenlaceCaso;
import bo.phishshield.backend.qa.domain.model.EstadoCaso;
import bo.phishshield.backend.qa.domain.model.EstadoPaso;
import bo.phishshield.backend.qa.domain.model.FinEjecucion;
import bo.phishshield.backend.qa.domain.model.PasoEjecutado;
import bo.phishshield.backend.qa.domain.model.PlanEjecucion;
import bo.phishshield.backend.qa.domain.model.Suite;
import bo.phishshield.backend.qa.domain.model.TipoFallo;
import bo.phishshield.backend.qa.domain.port.out.OyenteEjecucion;
import bo.phishshield.backend.qa.infrastructure.config.QaPropiedades;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Corre casos falsos con el JUnit Platform Launcher de verdad y comprueba
 * como se traduce cada desenlace.
 */
class JUnitEjecutorPruebasAdapterTest {

    private static final String BASE_QA = "jdbc:postgresql://localhost:5432/phishshield_qa";

    /** Oyente que anota todo lo que recibe. */
    static class Anotador implements OyenteEjecucion {
        final Map<String, DesenlaceCaso> terminados = new LinkedHashMap<>();
        final Map<String, String> noEjecutados = new LinkedHashMap<>();

        @Override
        public void casoTerminado(DesenlaceCaso desenlace) {
            terminados.put(desenlace.codigo(), desenlace);
        }

        @Override
        public void casoNoEjecutado(String codigo, String motivo) {
            noEjecutados.put(codigo, motivo);
        }
    }

    private static JUnitEjecutorPruebasAdapter adaptador(String urlBase, boolean permitirReal) {
        QaPropiedades propiedades = new QaPropiedades("http://localhost:8080", null, null, null,
                "target/qa-evidencias-prueba", "prueba", permitirReal);
        return new JUnitEjecutorPruebasAdapter(new RegistroCasosQA(List.of(CasosFalsosParaElEjecutor.class)),
                propiedades, "http://localhost:8000", urlBase, Clock.systemDefaultZone());
    }

    private static PlanEjecucion plan(String... codigos) {
        return new PlanEjecucion(UUID.randomUUID(), Suite.SMOKE, List.of(codigos), Map.of());
    }

    @Test
    void cada_desenlace_de_junit_se_traduce_al_estado_del_caso() {
        Anotador oyente = new Anotador();

        FinEjecucion fin = adaptador(BASE_QA, false).ejecutar(
                plan("TC-TST-001", "TC-TST-002", "TC-TST-003", "TC-TST-004", "TC-TST-005", "TC-TST-009"), oyente);

        assertEquals(FinEjecucion.COMPLETA, fin);

        DesenlaceCaso pasa = oyente.terminados.get("TC-TST-001");
        assertEquals(EstadoCaso.PASS, pasa.estado());
        assertEquals(1, pasa.pasos().size());
        assertEquals("todo bien", pasa.pasos().getFirst().resultadoObservado());

        DesenlaceCaso asercion = oyente.terminados.get("TC-TST-002");
        assertEquals(EstadoCaso.FAIL, asercion.estado());
        assertEquals(TipoFallo.FUNCIONAL, asercion.tipoFallo());
        assertEquals("Verificar el código HTTP", asercion.pasoFallido());
        assertEquals(EstadoPaso.FALLO, asercion.pasos().get(1).estado());
        assertTrue(asercion.pasos().get(1).resultadoObservado().startsWith("HTTP 500 | Código HTTP"));
        assertTrue(asercion.mensajeError().contains("expected: <200> but was: <500>"));

        DesenlaceCaso conexion = oyente.terminados.get("TC-TST-003");
        assertEquals(TipoFallo.INFRAESTRUCTURA, conexion.tipoFallo());

        assertTrue(oyente.noEjecutados.get("TC-TST-004").contains("QA_TEST_USER"),
                "Falta de configuración -> NOT_RUN con el motivo");

        DesenlaceCaso omitido = oyente.terminados.get("TC-TST-005");
        assertEquals(EstadoCaso.SKIPPED, omitido.estado());
        assertTrue(omitido.observacion().contains("No hay datos para comprobar"));

        assertEquals(JUnitEjecutorPruebasAdapter.NO_IMPLEMENTADO, oyente.noEjecutados.get("TC-TST-009"));
    }

    @Test
    void contra_la_base_real_no_corre_ningun_caso() {
        Anotador oyente = new Anotador();

        adaptador("jdbc:postgresql://localhost:5432/phishshield", false).ejecutar(plan("TC-TST-001"), oyente);

        assertTrue(oyente.terminados.isEmpty());
        assertTrue(oyente.noEjecutados.get("TC-TST-001").contains("base real"));
    }

    @Test
    void con_permiso_explicito_si_corre_contra_otra_base() {
        Anotador oyente = new Anotador();

        adaptador("jdbc:postgresql://localhost:5432/phishshield", true).ejecutar(plan("TC-TST-001"), oyente);

        assertEquals(EstadoCaso.PASS, oyente.terminados.get("TC-TST-001").estado());
    }

    @Test
    void detener_antes_de_empezar_deja_todo_sin_correr() {
        Anotador oyente = new Anotador();
        JUnitEjecutorPruebasAdapter ejecutor = adaptador(BASE_QA, false);
        PlanEjecucion plan = plan("TC-TST-001", "TC-TST-002");

        assertFalse(ejecutor.detener(plan.idEjecucion()), "Todavía no está corriendo");
        FinEjecucion fin = ejecutor.ejecutar(plan, oyente);

        assertEquals(FinEjecucion.DETENIDA, fin);
        assertTrue(oyente.terminados.isEmpty());
        assertEquals(JUnitEjecutorPruebasAdapter.DETENIDA, oyente.noEjecutados.get("TC-TST-002"));
    }

    @Test
    void reconoce_las_bases_de_prueba_por_su_nombre() {
        assertTrue(JUnitEjecutorPruebasAdapter.esBaseDePruebas(BASE_QA));
        assertTrue(JUnitEjecutorPruebasAdapter.esBaseDePruebas("jdbc:postgresql://localhost:5432/prueba_carga?ssl=false"));
        assertFalse(JUnitEjecutorPruebasAdapter.esBaseDePruebas("jdbc:postgresql://localhost:5432/phishshield"));
        assertFalse(JUnitEjecutorPruebasAdapter.esBaseDePruebas(null));
    }

    /** DEF-003: un PDF no se copia a la evidencia y un texto con caracter nulo se limpia antes de guardarlo. */
    @Test
    void las_respuestas_binarias_no_van_a_la_evidencia_y_el_caracter_nulo_se_limpia() {
        assertTrue(ClienteApiQA.esBinario("application/pdf"));
        assertTrue(ClienteApiQA.esBinario("image/png"));
        assertFalse(ClienteApiQA.esBinario("application/json;charset=UTF-8"));
        assertFalse(ClienteApiQA.esBinario(null));

        PasoEjecutado paso = new PasoEjecutado(1, "Descargar", "ok", EstadoPaso.OK, null,
                "GET /pdf\n-> HTTP 200\n%PDF-1.7 \u0000\u0000 binario", java.time.OffsetDateTime.now());
        assertFalse(paso.detalleApi().contains("\u0000"));
        assertTrue(paso.detalleApi().startsWith("GET /pdf"));
    }

    @Test
    void la_evidencia_de_api_nunca_lleva_contrasenas_ni_tokens() {
        String enmascarado = ClienteApiQA.enmascarar(
                "{\"email\":\"qa@phishshield.bo\",\"contrasena\":\"Secreta123!\",\"tokenAcceso\":\"eyJabc.def\","
                        + "\"tokenRefresco\":\"r4nd0m\"}");

        assertFalse(enmascarado.contains("Secreta123!"));
        assertFalse(enmascarado.contains("eyJabc"));
        assertFalse(enmascarado.contains("r4nd0m"));
        assertTrue(enmascarado.contains("\"email\":\"qa@phishshield.bo\""));
        assertTrue(enmascarado.contains("\"contrasena\":\"***\""));
    }
}
