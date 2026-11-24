package bo.phishshield.backend.qa.domain.model;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EjecucionTest {

    private static final OffsetDateTime CREADA = OffsetDateTime.parse("2026-11-17T21:00:00-04:00");
    private static final OffsetDateTime INICIO = CREADA.plusSeconds(2);
    private static final OffsetDateTime FIN = INICIO.plusSeconds(30);
    private static final CorreoDestino CORREO = new CorreoDestino("qa@phishshield.bo");

    static CasoPrueba caso(String codigo, Suite suite, int orden) {
        return new CasoPrueba((long) orden, codigo, suite, Herramienta.API, orden, "Caso " + codigo,
                "objetivo", "precondiciones", "datos", "esperado", true);
    }

    private static final CasoPrueba SMK1 = caso("TC-SMK-001", Suite.SMOKE, 1);
    private static final CasoPrueba SMK2 = caso("TC-SMK-002", Suite.SMOKE, 2);
    private static final CasoPrueba SMK3 = caso("TC-SMK-003", Suite.SMOKE, 3);

    private static Ejecucion nueva(CasoPrueba... casos) {
        return Ejecucion.nueva(1L, Suite.SMOKE, AlcanceEjecucion.SELECCION, CORREO, "local",
                Map.of("usuarios", "20"), List.of(casos), CREADA);
    }

    private static Ejecucion enCurso(CasoPrueba... casos) {
        Ejecucion e = nueva(casos);
        e.iniciar(INICIO);
        return e;
    }

    private static DesenlaceCaso pass(String codigo) {
        return DesenlaceCaso.aprobado(codigo, INICIO, INICIO.plusSeconds(1), List.of(), List.of());
    }

    private static DesenlaceCaso fail(String codigo) {
        return DesenlaceCaso.fallido(codigo, TipoFallo.FUNCIONAL, "expected: <200> but was: <500>\n\tat ...",
                "Consultar empresas", INICIO, INICIO.plusSeconds(1), List.of(), List.of());
    }

    // ------------------------------------------------------------------
    // Creacion
    // ------------------------------------------------------------------

    @Test
    void nueva_queda_pendiente_con_un_not_run_por_caso_en_el_orden_del_catalogo() {
        Ejecucion e = nueva(SMK3, SMK1, SMK2);

        assertEquals(EstadoEjecucion.PENDIENTE, e.getEstado());
        assertEquals(EstadoCorreo.PENDIENTE, e.getEstadoCorreo());
        assertEquals(TipoPrueba.FUNCIONAL, e.tipo());
        assertNotNull(e.getUuidPublico());
        assertEquals(List.of("TC-SMK-001", "TC-SMK-002", "TC-SMK-003"), e.codigos());
        assertTrue(e.getResultados().stream().allMatch(r -> r.getEstado() == EstadoCaso.NOT_RUN));
        assertEquals(List.of(1, 2, 3), e.getResultados().stream().map(ResultadoCaso::getOrden).toList());
        assertNull(e.getInicio());
        assertNull(e.duracionMs());
    }

    @Test
    void sin_casos_no_se_puede_crear() {
        assertThrows(SeleccionInvalidaException.class, () -> nueva());
    }

    @Test
    void un_caso_de_otra_suite_se_rechaza() {
        CasoPrueba deSanity = caso("TC-SAN-001", Suite.SANITY, 1);

        SeleccionInvalidaException ex = assertThrows(SeleccionInvalidaException.class, () -> nueva(SMK1, deSanity));
        assertTrue(ex.getMessage().contains("TC-SAN-001"));
    }

    @Test
    void un_caso_desactivado_se_rechaza() {
        CasoPrueba inactivo = new CasoPrueba(9L, "TC-SMK-009", Suite.SMOKE, Herramienta.API, 9, "x",
                "x", "x", "x", "x", false);

        assertThrows(SeleccionInvalidaException.class, () -> nueva(SMK1, inactivo));
    }

    @Test
    void un_caso_repetido_se_rechaza() {
        assertThrows(SeleccionInvalidaException.class, () -> nueva(SMK1, SMK1));
    }

    @Test
    void sin_ambiente_se_usa_local() {
        Ejecucion e = Ejecucion.nueva(1L, Suite.SMOKE, AlcanceEjecucion.SUITE_COMPLETA, CORREO, "  ",
                null, List.of(SMK1), CREADA);

        assertEquals("local", e.getAmbiente());
        assertEquals(Map.of(), e.getParametros());
    }

    // ------------------------------------------------------------------
    // Ciclo de vida y resultado global
    // ------------------------------------------------------------------

    @Test
    void con_un_fail_termina_finalizada_con_fallos() {
        Ejecucion e = enCurso(SMK1, SMK2);

        e.registrarResultado(pass("TC-SMK-001"));
        e.registrarResultado(fail("TC-SMK-002"));
        e.finalizar(FIN);

        assertEquals(EstadoEjecucion.FINALIZADA, e.getEstado());
        assertEquals(ResultadoGlobal.CON_FALLOS, e.getResultadoGlobal());
        assertEquals(30_000L, e.duracionMs());
        assertEquals(new ResumenEjecucion(2, 1, 1, 0, 0), e.resumen());
    }

    @Test
    void todos_pass_termina_aprobada() {
        Ejecucion e = enCurso(SMK1, SMK2);

        e.registrarResultado(pass("TC-SMK-001"));
        e.registrarResultado(pass("TC-SMK-002"));
        e.finalizar(FIN);

        assertEquals(ResultadoGlobal.APROBADA, e.getResultadoGlobal());
    }

    @Test
    void un_skipped_sin_fail_termina_incompleta() {
        Ejecucion e = enCurso(SMK1, SMK2);

        e.registrarResultado(pass("TC-SMK-001"));
        e.registrarResultado(DesenlaceCaso.omitido("TC-SMK-002", "QA_TEST_USER no configurado",
                INICIO, INICIO, List.of()));
        e.finalizar(FIN);

        assertEquals(ResultadoGlobal.INCOMPLETA, e.getResultadoGlobal());
    }

    @Test
    void solo_se_registran_resultados_con_la_ejecucion_en_curso() {
        Ejecucion pendiente = nueva(SMK1);
        assertThrows(TransicionEjecucionInvalidaException.class, () -> pendiente.registrarResultado(pass("TC-SMK-001")));

        Ejecucion terminada = enCurso(SMK1);
        terminada.registrarResultado(pass("TC-SMK-001"));
        terminada.finalizar(FIN);
        assertThrows(TransicionEjecucionInvalidaException.class, () -> terminada.registrarResultado(pass("TC-SMK-001")));
    }

    @Test
    void un_caso_que_no_es_de_la_ejecucion_se_rechaza() {
        Ejecucion e = enCurso(SMK1);

        assertThrows(IllegalArgumentException.class, () -> e.registrarResultado(pass("TC-SMK-002")));
    }

    @Test
    void un_caso_no_recibe_dos_desenlaces() {
        Ejecucion e = enCurso(SMK1);
        e.registrarResultado(pass("TC-SMK-001"));

        assertThrows(IllegalStateException.class, () -> e.registrarResultado(fail("TC-SMK-001")));
        assertEquals(EstadoCaso.PASS, e.getResultados().getFirst().getEstado(), "el primer desenlace se conserva");
    }

    @Test
    void si_falla_a_mitad_los_casos_restantes_quedan_not_run_con_el_motivo() {
        Ejecucion e = enCurso(SMK1, SMK2, SMK3);
        e.registrarResultado(pass("TC-SMK-001"));

        e.fallar("El backend se reinició durante la ejecución", FIN);

        assertEquals(EstadoEjecucion.FALLIDA, e.getEstado());
        assertNull(e.getResultadoGlobal(), "una ejecución fallida no tiene resultado global");
        assertEquals("El backend se reinició durante la ejecución", e.getMensajeError());
        assertEquals(new ResumenEjecucion(3, 1, 0, 0, 2), e.resumen());
        ResultadoCaso restante = e.getResultados().get(2);
        assertEquals(EstadoCaso.NOT_RUN, restante.getEstado());
        assertEquals("No se ejecutó: El backend se reinició durante la ejecución", restante.observacionResumida());
        assertEquals("Sin observaciones", e.getResultados().getFirst().observacionResumida());
    }

    @Test
    void puede_fallar_antes_de_empezar() {
        Ejecucion e = nueva(SMK1);

        e.fallar("No se pudo iniciar el motor de pruebas", FIN);

        assertEquals(EstadoEjecucion.FALLIDA, e.getEstado());
        assertNull(e.getInicio());
        assertEquals(FIN, e.getFin());
    }

    @Test
    void las_transiciones_fuera_del_ciclo_se_rechazan() {
        Ejecucion pendiente = nueva(SMK1);
        assertThrows(TransicionEjecucionInvalidaException.class, () -> pendiente.finalizar(FIN));

        Ejecucion enCurso = enCurso(SMK1);
        assertThrows(TransicionEjecucionInvalidaException.class, () -> enCurso.iniciar(INICIO));

        Ejecucion fallida = enCurso(SMK1);
        fallida.fallar("x", FIN);
        assertThrows(TransicionEjecucionInvalidaException.class, () -> fallida.finalizar(FIN));
        assertThrows(TransicionEjecucionInvalidaException.class, () -> fallida.fallar("otra vez", FIN));
    }

    @Test
    void la_tabla_de_transiciones_es_la_del_ciclo_de_vida() {
        assertTrue(EstadoEjecucion.PENDIENTE.puedePasarA(EstadoEjecucion.EN_CURSO));
        assertTrue(EstadoEjecucion.PENDIENTE.puedePasarA(EstadoEjecucion.FALLIDA));
        assertFalse(EstadoEjecucion.PENDIENTE.puedePasarA(EstadoEjecucion.FINALIZADA));
        assertTrue(EstadoEjecucion.EN_CURSO.puedePasarA(EstadoEjecucion.FINALIZADA));
        assertTrue(EstadoEjecucion.EN_CURSO.puedePasarA(EstadoEjecucion.FALLIDA));
        assertFalse(EstadoEjecucion.EN_CURSO.puedePasarA(EstadoEjecucion.PENDIENTE));
        assertTrue(EstadoEjecucion.FINALIZADA.esFinal());
        assertTrue(EstadoEjecucion.FALLIDA.esFinal());
        assertTrue(EstadoEjecucion.PENDIENTE.estaActiva());
        assertTrue(EstadoEjecucion.EN_CURSO.estaActiva());
    }

    @Test
    void el_fin_nunca_queda_antes_del_inicio() {
        Ejecucion e = enCurso(SMK1);
        e.registrarResultado(pass("TC-SMK-001"));

        e.finalizar(INICIO.minusSeconds(5));

        assertEquals(INICIO, e.getFin(), "chk_qa_ejec_fechas lo exige");
        assertEquals(0L, e.duracionMs());
    }

    // ------------------------------------------------------------------
    // Reportes y correo
    // ------------------------------------------------------------------

    @Test
    void reportes_y_correo_solo_cuando_la_ejecucion_termino() {
        Ejecucion e = enCurso(SMK1);
        ArchivosReporte archivos = new ArchivosReporte("x/reporte.pdf", "x/reporte.html");

        assertThrows(TransicionEjecucionInvalidaException.class, () -> e.registrarReportes(archivos));
        assertThrows(TransicionEjecucionInvalidaException.class, () -> e.marcarCorreoEnviado(FIN));
    }

    @Test
    void un_error_de_correo_queda_registrado_y_los_reportes_siguen_disponibles() {
        Ejecucion e = enCurso(SMK1);
        e.registrarResultado(pass("TC-SMK-001"));
        e.finalizar(FIN);
        e.registrarReportes(new ArchivosReporte("x/reporte.pdf", "x/reporte.html"));

        e.marcarErrorCorreo("535 Authentication failed");

        assertEquals(EstadoCorreo.ERROR, e.getEstadoCorreo());
        assertEquals("535 Authentication failed", e.getErrorCorreo());
        assertTrue(e.tieneReportes());
        assertEquals(EstadoEjecucion.FINALIZADA, e.getEstado(), "el correo no cambia el resultado de las pruebas");

        e.marcarCorreoEnviado(FIN.plusMinutes(1));
        assertEquals(EstadoCorreo.ENVIADO, e.getEstadoCorreo());
        assertNull(e.getErrorCorreo(), "un reenvío exitoso limpia el error anterior");
    }

    @Test
    void sin_smtp_se_marca_no_configurado() {
        Ejecucion e = enCurso(SMK1);
        e.fallar("x", FIN);

        e.marcarCorreoNoConfigurado();

        assertEquals(EstadoCorreo.NO_CONFIGURADO, e.getEstadoCorreo());
        assertTrue(e.getErrorCorreo().contains("MAIL_HOST"));
    }

    @Test
    void el_asunto_usa_la_suite_y_la_hora_de_bolivia() {
        Ejecucion e = nueva(SMK1);
        e.iniciar(OffsetDateTime.parse("2026-11-18T01:05:00Z"));

        assertEquals("Reporte de automatización – Smoke – 17/11/2026 21:05",
                e.asuntoCorreo(ZoneId.of("America/La_Paz")));
    }

    // ------------------------------------------------------------------
    // Observaciones y plan
    // ------------------------------------------------------------------

    @Test
    void la_observacion_de_un_fail_dice_tipo_paso_y_primera_linea_del_error() {
        Ejecucion e = enCurso(SMK1);
        e.registrarResultado(fail("TC-SMK-001"));

        assertEquals("Fallo funcional en el paso \"Consultar empresas\": expected: <200> but was: <500>",
                e.getResultados().getFirst().observacionResumida());
    }

    @Test
    void el_plan_lleva_la_ejecucion_los_codigos_en_orden_y_los_parametros() {
        Ejecucion e = nueva(SMK2, SMK1);

        PlanEjecucion plan = PlanEjecucion.de(e);

        assertEquals(e.getUuidPublico(), plan.idEjecucion());
        assertEquals(Suite.SMOKE, plan.suite());
        assertEquals(List.of("TC-SMK-001", "TC-SMK-002"), plan.codigos());
        assertEquals("20", plan.parametros().get("usuarios"));
    }

    @Test
    void solo_se_arma_contexto_de_ia_para_casos_fallidos() {
        Ejecucion e = enCurso(SMK1, SMK2);
        e.registrarResultado(pass("TC-SMK-001"));
        e.registrarResultado(fail("TC-SMK-002"));

        assertThrows(IllegalArgumentException.class, () -> ContextoFallo.de(e.getResultados().get(0)));
        ContextoFallo contexto = ContextoFallo.de(e.getResultados().get(1));
        assertEquals("TC-SMK-002", contexto.codigo());
        assertEquals("Consultar empresas", contexto.pasoFallido());
    }
}
