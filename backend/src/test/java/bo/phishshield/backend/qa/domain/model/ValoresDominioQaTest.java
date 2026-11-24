package bo.phishshield.backend.qa.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/** Objetos de valor y enums de la vertical qa. */
class ValoresDominioQaTest {

    private static final OffsetDateTime T0 = OffsetDateTime.parse("2026-11-17T21:00:00-04:00");

    // ------------------------------------------------------------------
    // Suites y tipos
    // ------------------------------------------------------------------

    @Test
    void cada_suite_pertenece_al_tipo_que_pide_la_materia() {
        assertEquals(List.of(Suite.SMOKE, Suite.SANITY, Suite.REGRESSION, Suite.INTEGRATION, Suite.E2E),
                Suite.delTipo(TipoPrueba.FUNCIONAL));
        assertEquals(List.of(Suite.PERFORMANCE, Suite.ESTRES, Suite.SOAK, Suite.ROBUSTEZ_IA),
                Suite.delTipo(TipoPrueba.NO_FUNCIONAL));
    }

    @Test
    void todos_los_enums_van_y_vuelven_de_la_base_en_minusculas() {
        idaYVuelta(TipoPrueba.values(), TipoPrueba::aValorBd, TipoPrueba::desdeValorBd);
        idaYVuelta(Suite.values(), Suite::aValorBd, Suite::desdeValorBd);
        idaYVuelta(Herramienta.values(), Herramienta::aValorBd, Herramienta::desdeValorBd);
        idaYVuelta(EstadoEjecucion.values(), EstadoEjecucion::aValorBd, EstadoEjecucion::desdeValorBd);
        idaYVuelta(EstadoCaso.values(), EstadoCaso::aValorBd, EstadoCaso::desdeValorBd);
        idaYVuelta(TipoFallo.values(), TipoFallo::aValorBd, TipoFallo::desdeValorBd);
        idaYVuelta(ResultadoGlobal.values(), ResultadoGlobal::aValorBd, ResultadoGlobal::desdeValorBd);
        idaYVuelta(EstadoCorreo.values(), EstadoCorreo::aValorBd, EstadoCorreo::desdeValorBd);
        idaYVuelta(AlcanceEjecucion.values(), AlcanceEjecucion::aValorBd, AlcanceEjecucion::desdeValorBd);
        idaYVuelta(EstadoPaso.values(), EstadoPaso::aValorBd, EstadoPaso::desdeValorBd);

        assertEquals("not_run", EstadoCaso.NOT_RUN.aValorBd());
        assertEquals("robustez_ia", Suite.ROBUSTEZ_IA.aValorBd());
    }

    private static <E> void idaYVuelta(E[] valores, Function<E, String> aBd, Function<String, E> desdeBd) {
        for (E valor : valores) {
            String enBd = aBd.apply(valor);
            assertEquals(enBd.toLowerCase(), enBd);
            assertEquals(valor, desdeBd.apply(enBd));
        }
    }

    @Test
    void un_valor_desconocido_de_la_base_se_rechaza_con_los_validos() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> EstadoCaso.desdeValorBd("positivo"));
        assertTrue(ex.getMessage().contains("pass, fail, skipped, not_run"));
        assertThrows(IllegalArgumentException.class, () -> Suite.desdeValorBd(" "));
    }

    // ------------------------------------------------------------------
    // Correo
    // ------------------------------------------------------------------

    @Test
    void el_correo_se_limpia_y_se_guarda_en_minusculas() {
        assertEquals("marcos.vargas@umsa.bo", new CorreoDestino("  Marcos.Vargas@UMSA.bo ").valor());
        assertEquals("qa+demo@phishshield.com.bo", new CorreoDestino("qa+demo@phishshield.com.bo").valor());
    }

    @ParameterizedTest(name = "\"{0}\" se rechaza")
    @ValueSource(strings = {"", "   ", "sinarroba.com", "a@b", "a@b.c", "a b@c.com", "a@@b.com",
            "a..b@c.com", "@dominio.com", "usuario@", "usuario@dominio.", "usuario@-.com.", "ñandu@correo.bo"})
    void los_correos_mal_formados_se_rechazan(String correo) {
        assertThrows(CorreoInvalidoException.class, () -> new CorreoDestino(correo));
    }

    @Test
    void un_correo_vacio_o_demasiado_largo_se_rechaza() {
        assertThrows(CorreoInvalidoException.class, () -> new CorreoDestino(null));
        String largo = "a".repeat(140) + "@correo.com";
        assertThrows(CorreoInvalidoException.class, () -> new CorreoDestino(largo));
    }

    // ------------------------------------------------------------------
    // Catalogo
    // ------------------------------------------------------------------

    @Test
    void el_codigo_del_caso_debe_tener_el_formato_y_el_prefijo_de_su_suite() {
        assertDoesNotThrow(() -> caso("TC-PERF-001", Suite.PERFORMANCE));
        assertThrows(IllegalArgumentException.class, () -> caso("SMK-001", Suite.SMOKE));
        assertThrows(IllegalArgumentException.class, () -> caso("TC-SMK-1", Suite.SMOKE));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> caso("TC-SMK-001", Suite.SANITY));
        assertTrue(ex.getMessage().contains("TC-SAN-"));
    }

    @Test
    void el_caso_exige_sus_metadatos_y_toma_el_tipo_de_la_suite() {
        assertThrows(IllegalArgumentException.class, () -> new CasoPrueba(1L, "TC-SMK-001", Suite.SMOKE,
                Herramienta.API, 1, "nombre", "objetivo", "  ", "datos", "esperado", true));
        assertEquals(TipoPrueba.NO_FUNCIONAL, caso("TC-ROB-001", Suite.ROBUSTEZ_IA).tipo());
    }

    private static CasoPrueba caso(String codigo, Suite suite) {
        return new CasoPrueba(1L, codigo, suite, Herramienta.API, 1, "n", "o", "p", "d", "e", true);
    }

    // ------------------------------------------------------------------
    // Resultado global y resumen
    // ------------------------------------------------------------------

    @Test
    void el_resultado_global_sigue_la_regla_aprobada_con_fallos_incompleta() {
        assertEquals(ResultadoGlobal.APROBADA, ResultadoGlobal.calcular(List.of(EstadoCaso.PASS, EstadoCaso.PASS)));
        assertEquals(ResultadoGlobal.CON_FALLOS,
                ResultadoGlobal.calcular(List.of(EstadoCaso.PASS, EstadoCaso.FAIL, EstadoCaso.SKIPPED)));
        assertEquals(ResultadoGlobal.INCOMPLETA, ResultadoGlobal.calcular(List.of(EstadoCaso.PASS, EstadoCaso.NOT_RUN)));
        assertEquals(ResultadoGlobal.INCOMPLETA, ResultadoGlobal.calcular(List.of()));
    }

    @Test
    void el_porcentaje_de_exito_es_pass_sobre_total_con_un_decimal() {
        assertEquals(new BigDecimal("37.5"), new ResumenEjecucion(8, 3, 2, 2, 1).porcentajeExito());
        assertEquals(new BigDecimal("66.7"), new ResumenEjecucion(3, 2, 1, 0, 0).porcentajeExito());
        assertEquals(new BigDecimal("0.0"), new ResumenEjecucion(0, 0, 0, 0, 0).porcentajeExito());
        assertEquals(7, new ResumenEjecucion(8, 3, 2, 2, 1).terminados());
        assertThrows(IllegalArgumentException.class, () -> new ResumenEjecucion(5, 1, 1, 1, 1));
    }

    // ------------------------------------------------------------------
    // Metricas
    // ------------------------------------------------------------------

    @Test
    void un_tiempo_cumple_solo_si_queda_por_debajo_del_umbral() {
        BigDecimal umbral = new BigDecimal("2000");
        assertTrue(MetricaRendimiento.conMaximo("p95", new BigDecimal("1999.9"), "ms", umbral).cumple());
        assertFalse(MetricaRendimiento.conMaximo("p95", new BigDecimal("2000"), "ms", umbral).cumple());
    }

    @Test
    void precision_y_recall_cumplen_si_alcanzan_el_umbral() {
        BigDecimal umbral = new BigDecimal("0.80");
        assertTrue(MetricaRendimiento.conMinimo("recall", new BigDecimal("0.80"), "proporción", umbral).cumple());
        assertTrue(MetricaRendimiento.conMinimo("recall", new BigDecimal("0.79"), "proporción", umbral).incumple());
    }

    @Test
    void sin_umbral_no_se_puede_decir_si_cumple() {
        assertThrows(IllegalArgumentException.class,
                () -> new MetricaRendimiento("p95", BigDecimal.ONE, "ms", null, true, null));
        assertThrows(IllegalArgumentException.class,
                () -> new MetricaRendimiento("p95", BigDecimal.ONE, "ms", BigDecimal.TEN, null, null));
        MetricaRendimiento informativa = MetricaRendimiento.informativa("throughput", BigDecimal.TEN, "req/s");
        assertNull(informativa.cumple());
        assertFalse(informativa.incumple());
        assertEquals(3, informativa.enEscalon(3).escalon());
    }

    // ------------------------------------------------------------------
    // Desenlaces y pasos
    // ------------------------------------------------------------------

    @Test
    void un_desenlace_no_puede_ser_not_run_ni_contradecir_el_tipo_de_fallo() {
        assertThrows(IllegalArgumentException.class, () -> new DesenlaceCaso("TC-SMK-001", EstadoCaso.NOT_RUN,
                null, null, null, null, T0, T0, null, null));
        assertThrows(IllegalArgumentException.class, () -> new DesenlaceCaso("TC-SMK-001", EstadoCaso.FAIL,
                null, "x", null, null, T0, T0, null, null));
        assertThrows(IllegalArgumentException.class, () -> new DesenlaceCaso("TC-SMK-001", EstadoCaso.PASS,
                TipoFallo.FUNCIONAL, null, null, null, T0, T0, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> DesenlaceCaso.aprobado("TC-SMK-001", T0, T0.minusSeconds(1), null, null));
    }

    @Test
    void un_mensaje_de_error_enorme_se_recorta() {
        DesenlaceCaso d = DesenlaceCaso.fallido("TC-SMK-001", TipoFallo.INFRAESTRUCTURA, "x".repeat(10_000),
                "Paso", T0, T0, null, null);

        assertEquals(4000, d.mensajeError().length());
        assertTrue(d.mensajeError().endsWith("…"));
        assertEquals(List.of(), d.pasos());
    }

    @Test
    void un_paso_se_numera_desde_1_y_su_descripcion_entra_en_la_columna() {
        assertThrows(IllegalArgumentException.class,
                () -> new PasoEjecutado(0, "Abrir login", null, EstadoPaso.OK, null, null, T0));
        PasoEjecutado paso = new PasoEjecutado(1, "d".repeat(300), "  ", EstadoPaso.FALLO, " ", null, T0);
        assertEquals(PasoEjecutado.MAXIMO_DESCRIPCION, paso.descripcion().length());
        assertNull(paso.resultadoObservado());
        assertFalse(paso.tieneCaptura());
        assertTrue(paso.fallo());
    }
}
