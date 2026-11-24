package bo.phishshield.backend.qa.application;

import bo.phishshield.backend.qa.dobles.DoblesQa;
import bo.phishshield.backend.qa.dobles.DoblesQa.CatalogoEnMemoria;
import bo.phishshield.backend.qa.dobles.DoblesQa.EjecucionesEnMemoria;
import bo.phishshield.backend.qa.dobles.DoblesQa.EjecutorGuionado;
import bo.phishshield.backend.qa.dobles.DoblesQa.PlanificadorQueAnota;
import bo.phishshield.backend.qa.domain.model.AlcanceEjecucion;
import bo.phishshield.backend.qa.domain.model.CasoCatalogo;
import bo.phishshield.backend.qa.domain.model.CasoPrueba;
import bo.phishshield.backend.qa.domain.model.CorreoInvalidoException;
import bo.phishshield.backend.qa.domain.model.DesenlaceCaso;
import bo.phishshield.backend.qa.domain.model.Ejecucion;
import bo.phishshield.backend.qa.domain.model.EjecucionEnCursoException;
import bo.phishshield.backend.qa.domain.model.EjecucionNoEncontradaException;
import bo.phishshield.backend.qa.domain.model.EstadoCaso;
import bo.phishshield.backend.qa.domain.model.EstadoEjecucion;
import bo.phishshield.backend.qa.domain.model.FinEjecucion;
import bo.phishshield.backend.qa.domain.model.ResultadoCaso;
import bo.phishshield.backend.qa.domain.model.ResultadoGlobal;
import bo.phishshield.backend.qa.domain.model.SeleccionInvalidaException;
import bo.phishshield.backend.qa.domain.model.Suite;
import bo.phishshield.backend.qa.domain.model.TipoFallo;
import bo.phishshield.backend.qa.domain.model.TransicionEjecucionInvalidaException;
import bo.phishshield.backend.shared.domain.model.AccesoDenegadoException;
import bo.phishshield.backend.shared.domain.model.Rol;
import bo.phishshield.backend.shared.domain.model.SolicitudPagina;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Casos de uso del Centro de Pruebas con dobles en memoria, sin Spring. */
class CasosDeUsoQaTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-24T15:00:00Z"), ZoneOffset.ofHours(-4));
    private static final OffsetDateTime AHORA = OffsetDateTime.now(RELOJ);

    private static final UsuarioActual ADMIN =
            new UsuarioActual(1L, UUID.randomUUID(), "admin@phishshield.bo", null, Rol.ADMIN);
    private static final UsuarioActual ADMIN_EMPRESA =
            new UsuarioActual(2L, UUID.randomUUID(), "maria@andina.bo", 1L, Rol.ADMIN_EMPRESA);

    private static final CasoPrueba SMK1 = DoblesQa.caso("TC-SMK-001", Suite.SMOKE, 1);
    private static final CasoPrueba SMK2 = DoblesQa.caso("TC-SMK-002", Suite.SMOKE, 2);
    private static final CasoPrueba SMK3 = DoblesQa.caso("TC-SMK-003", Suite.SMOKE, 3);
    private static final CasoPrueba SAN1 = DoblesQa.caso("TC-SAN-001", Suite.SANITY, 1);

    private final CatalogoEnMemoria catalogo = new CatalogoEnMemoria(SMK1, SMK2, SMK3, SAN1);
    private final EjecucionesEnMemoria ejecuciones = new EjecucionesEnMemoria();
    private final PlanificadorQueAnota planificador = new PlanificadorQueAnota();
    private final EjecutorGuionado ejecutor = new EjecutorGuionado();
    private final List<Ejecucion> terminadas = new ArrayList<>();

    private final IniciarEjecucionUseCase iniciar =
            new IniciarEjecucionUseCase(catalogo, ejecuciones, planificador, "phishshield_qa", RELOJ);
    private final CorrerEjecucionUseCase correr =
            new CorrerEjecucionUseCase(ejecuciones, ejecutor, terminadas::add, RELOJ);
    private final DetenerEjecucionUseCase detener = new DetenerEjecucionUseCase(ejecuciones, ejecutor, RELOJ);

    private Ejecucion lanzarSmoke() {
        return iniciar.ejecutar(ADMIN, new ComandoIniciarEjecucion(Suite.SMOKE, List.of(), null, Map.of()));
    }

    // ------------------------------------------------------------------
    // IniciarEjecucion
    // ------------------------------------------------------------------

    @Test
    void lanzar_una_suite_crea_la_ejecucion_pendiente_y_la_programa() {
        Ejecucion e = lanzarSmoke();

        assertEquals(EstadoEjecucion.PENDIENTE, e.getEstado());
        assertEquals(AlcanceEjecucion.SUITE_COMPLETA, e.getAlcance());
        assertEquals(List.of("TC-SMK-001", "TC-SMK-002", "TC-SMK-003"), e.codigos());
        assertEquals("phishshield_qa", e.getAmbiente());
        assertEquals(List.of(e.getUuidPublico()), planificador.programadas);
        assertNotNull(e.getId(), "Debe devolver la ejecución ya guardada");
    }

    @Test
    void sin_correo_los_reportes_van_a_quien_la_lanza() {
        assertEquals("admin@phishshield.bo", lanzarSmoke().getCorreo().valor());
    }

    @Test
    void un_correo_invalido_se_rechaza() {
        assertThrows(CorreoInvalidoException.class, () -> iniciar.ejecutar(ADMIN,
                new ComandoIniciarEjecucion(Suite.SMOKE, List.of(), "no-es-correo", Map.of())));
        assertTrue(planificador.programadas.isEmpty());
    }

    @Test
    void lanzar_por_codigos_deduce_la_suite_y_marca_el_alcance_seleccion() {
        Ejecucion e = iniciar.ejecutar(ADMIN,
                new ComandoIniciarEjecucion(null, List.of("tc-smk-003", "TC-SMK-001"), "QA@Phishshield.bo", Map.of()));

        assertEquals(Suite.SMOKE, e.getSuite());
        assertEquals(AlcanceEjecucion.SELECCION, e.getAlcance());
        assertEquals(List.of("TC-SMK-001", "TC-SMK-003"), e.codigos(), "En el orden del catálogo");
        assertEquals("qa@phishshield.bo", e.getCorreo().valor());
    }

    @Test
    void un_codigo_inexistente_o_repetido_se_rechaza() {
        SeleccionInvalidaException inexistente = assertThrows(SeleccionInvalidaException.class, () -> iniciar.ejecutar(
                ADMIN, new ComandoIniciarEjecucion(null, List.of("TC-SMK-001", "TC-SMK-099"), null, Map.of())));
        assertTrue(inexistente.getMessage().contains("TC-SMK-099"));

        assertThrows(SeleccionInvalidaException.class, () -> iniciar.ejecutar(ADMIN,
                new ComandoIniciarEjecucion(null, List.of("TC-SMK-001", "tc-smk-001"), null, Map.of())));
    }

    @Test
    void casos_de_dos_suites_no_se_mezclan() {
        assertThrows(SeleccionInvalidaException.class, () -> iniciar.ejecutar(ADMIN,
                new ComandoIniciarEjecucion(null, List.of("TC-SMK-001", "TC-SAN-001"), null, Map.of())));
    }

    @Test
    void sin_suite_ni_codigos_no_hay_nada_que_ejecutar() {
        assertThrows(SeleccionInvalidaException.class, () -> iniciar.ejecutar(ADMIN,
                new ComandoIniciarEjecucion(null, List.of(), null, Map.of())));
    }

    @Test
    void una_suite_sin_casos_activos_se_rechaza() {
        assertThrows(SeleccionInvalidaException.class, () -> iniciar.ejecutar(ADMIN,
                new ComandoIniciarEjecucion(Suite.SOAK, List.of(), null, Map.of())));
    }

    @Test
    void los_parametros_se_validan() {
        assertThrows(SeleccionInvalidaException.class, () -> iniciar.ejecutar(ADMIN,
                new ComandoIniciarEjecucion(Suite.SMOKE, List.of(), null, Map.of("con espacio", "1"))));

        Ejecucion e = iniciar.ejecutar(ADMIN,
                new ComandoIniciarEjecucion(Suite.SMOKE, List.of(), null, Map.of("usuarios", " 10 ")));
        assertEquals(Map.of("usuarios", "10"), e.getParametros());
    }

    @Test
    void solo_se_admite_una_ejecucion_activa() {
        Ejecucion primera = lanzarSmoke();

        EjecucionEnCursoException ex = assertThrows(EjecucionEnCursoException.class, this::lanzarSmoke);
        assertTrue(ex.getMessage().contains(primera.getUuidPublico().toString()));
        assertEquals(1, planificador.programadas.size());
    }

    @Test
    void el_admin_de_empresa_no_usa_el_centro_de_pruebas() {
        ComandoIniciarEjecucion comando = new ComandoIniciarEjecucion(Suite.SMOKE, List.of(), null, Map.of());
        assertThrows(AccesoDenegadoException.class, () -> iniciar.ejecutar(ADMIN_EMPRESA, comando));
        assertThrows(AccesoDenegadoException.class,
                () -> new ListarEjecucionesUseCase(ejecuciones).ejecutar(ADMIN_EMPRESA, new SolicitudPagina(0, 20)));
        assertThrows(AccesoDenegadoException.class,
                () -> new ListarCatalogoUseCase(catalogo, ejecutor).ejecutar(ADMIN_EMPRESA));
        assertThrows(AccesoDenegadoException.class,
                () -> new ObtenerDetalleEjecucionUseCase(ejecuciones).ejecutar(ADMIN_EMPRESA, UUID.randomUUID()));
        assertThrows(AccesoDenegadoException.class, () -> detener.ejecutar(ADMIN_EMPRESA, UUID.randomUUID()));
    }

    // ------------------------------------------------------------------
    // CorrerEjecucion
    // ------------------------------------------------------------------

    @Test
    void correr_guarda_cada_desenlace_y_finaliza_con_el_resultado_global() {
        Ejecucion e = lanzarSmoke();
        List<EstadoEjecucion> estadosVistos = new ArrayList<>();
        ejecutor.guion = (plan, oyente) -> {
            estadosVistos.add(ejecuciones.unica().getEstado());
            oyente.casoTerminado(DesenlaceCaso.aprobado("TC-SMK-001", AHORA, AHORA.plusSeconds(1), List.of(), List.of()));
            // El avance ya esta en la "base" antes de que termine la ejecucion
            estadosVistos.add(ejecuciones.unica().getResultados().getFirst().getEstado() == EstadoCaso.PASS
                    ? EstadoEjecucion.EN_CURSO : EstadoEjecucion.PENDIENTE);
            oyente.casoTerminado(DesenlaceCaso.fallido("TC-SMK-002", TipoFallo.FUNCIONAL, "expected 200 but was 500",
                    "Consultar", AHORA, AHORA.plusSeconds(2), List.of(), List.of()));
            oyente.casoNoEjecutado("TC-SMK-003", "Se implementa en una fase posterior");
        };

        correr.ejecutar(e.getUuidPublico());

        Ejecucion final_ = ejecuciones.unica();
        assertEquals(List.of(EstadoEjecucion.EN_CURSO, EstadoEjecucion.EN_CURSO), estadosVistos);
        assertEquals(EstadoEjecucion.FINALIZADA, final_.getEstado());
        assertEquals(ResultadoGlobal.CON_FALLOS, final_.getResultadoGlobal());
        List<ResultadoCaso> r = final_.getResultados();
        assertEquals(EstadoCaso.PASS, r.get(0).getEstado());
        assertEquals(EstadoCaso.FAIL, r.get(1).getEstado());
        assertEquals(EstadoCaso.NOT_RUN, r.get(2).getEstado());
        assertEquals("Se implementa en una fase posterior", r.get(2).getObservacion());
        assertEquals(1, terminadas.size(), "Los pasos posteriores corren una vez");
    }

    @Test
    void si_todos_pasan_queda_aprobada() {
        Ejecucion e = lanzarSmoke();
        ejecutor.guion = DoblesQa.todosPass(AHORA);

        correr.ejecutar(e.getUuidPublico());

        assertEquals(ResultadoGlobal.APROBADA, ejecuciones.unica().getResultadoGlobal());
    }

    @Test
    void una_ejecucion_detenida_queda_fallida_y_conserva_el_motivo_de_cada_caso() {
        Ejecucion e = lanzarSmoke();
        ejecutor.guion = (plan, oyente) -> {
            oyente.casoTerminado(DesenlaceCaso.aprobado("TC-SMK-001", AHORA, AHORA.plusSeconds(1), List.of(), List.of()));
            oyente.casoNoEjecutado("TC-SMK-002", "La ejecución se detuvo antes de llegar a este caso");
        };
        ejecutor.fin = FinEjecucion.DETENIDA;

        correr.ejecutar(e.getUuidPublico());

        Ejecucion final_ = ejecuciones.unica();
        assertEquals(EstadoEjecucion.FALLIDA, final_.getEstado());
        assertNull(final_.getResultadoGlobal());
        assertEquals("Detenida por el usuario", final_.getMensajeError());
        assertEquals("La ejecución se detuvo antes de llegar a este caso",
                final_.getResultados().get(1).getObservacion());
        assertTrue(final_.getResultados().get(2).getObservacion().startsWith("No se ejecutó"));
        assertEquals(1, terminadas.size(), "Una detenida también genera sus reportes");
    }

    @Test
    void si_el_motor_falla_la_ejecucion_queda_fallida_con_el_error() {
        Ejecucion e = lanzarSmoke();
        ejecutor.error = new IllegalStateException("no se encontró el motor de JUnit");

        correr.ejecutar(e.getUuidPublico());

        Ejecucion final_ = ejecuciones.unica();
        assertEquals(EstadoEjecucion.FALLIDA, final_.getEstado());
        assertTrue(final_.getMensajeError().contains("no se encontró el motor de JUnit"));
    }

    /** DEF-003: un guardado final que falla no puede dejar la ejecucion activa para siempre. */
    @Test
    void si_no_se_puede_guardar_el_final_la_ejecucion_queda_fallida_y_no_bloquea() {
        Ejecucion e = lanzarSmoke();
        ejecutor.guion = DoblesQa.todosPass(AHORA);
        ejecuciones.fallaAlGuardar = new IllegalStateException("invalid byte sequence for encoding UTF8: 0x00");

        correr.ejecutar(e.getUuidPublico());

        Ejecucion final_ = ejecuciones.unica();
        assertEquals(EstadoEjecucion.FALLIDA, final_.getEstado());
        assertTrue(final_.getMensajeError().contains("No se pudo guardar el resultado"));
        assertTrue(ejecuciones.buscarActiva().isEmpty(), "Se puede lanzar otra");
        assertTrue(terminadas.isEmpty(), "Sin resultado guardado no se generan reportes");
    }

    @Test
    void una_ejecucion_que_ya_no_esta_pendiente_no_se_corre() {
        Ejecucion e = lanzarSmoke();
        detener.ejecutar(ADMIN, e.getUuidPublico());
        ejecutor.guion = (plan, oyente) -> fail("No debía correr");

        correr.ejecutar(e.getUuidPublico());
        correr.ejecutar(UUID.randomUUID());

        assertTrue(terminadas.isEmpty());
    }

    // ------------------------------------------------------------------
    // DetenerEjecucion
    // ------------------------------------------------------------------

    @Test
    void detener_una_pendiente_la_marca_fallida_enseguida() {
        Ejecucion e = lanzarSmoke();

        Ejecucion detenida = detener.ejecutar(ADMIN, e.getUuidPublico());

        assertEquals(EstadoEjecucion.FALLIDA, detenida.getEstado());
        assertEquals("Detenida por el usuario antes de empezar", detenida.getMensajeError());
        assertEquals(List.of(e.getUuidPublico()), ejecutor.detenidas);
        assertTrue(ejecuciones.buscarActiva().isEmpty(), "Ya se puede lanzar otra");
    }

    @Test
    void detener_una_que_esta_corriendo_solo_avisa_al_ejecutor() {
        Ejecucion e = lanzarSmoke();
        ejecutor.corriendo = true;
        ejecutor.guion = (plan, oyente) -> {
            Ejecucion vista = detener.ejecutar(ADMIN, plan.idEjecucion());
            assertEquals(EstadoEjecucion.EN_CURSO, vista.getEstado(), "La deja terminar el caso actual");
        };
        ejecutor.fin = FinEjecucion.DETENIDA;

        correr.ejecutar(e.getUuidPublico());

        assertEquals(EstadoEjecucion.FALLIDA, ejecuciones.unica().getEstado());
    }

    @Test
    void detener_una_terminada_o_inexistente_es_un_error() {
        Ejecucion e = lanzarSmoke();
        ejecutor.guion = DoblesQa.todosPass(AHORA);
        correr.ejecutar(e.getUuidPublico());

        assertThrows(TransicionEjecucionInvalidaException.class, () -> detener.ejecutar(ADMIN, e.getUuidPublico()));
        assertThrows(EjecucionNoEncontradaException.class, () -> detener.ejecutar(ADMIN, UUID.randomUUID()));
    }

    // ------------------------------------------------------------------
    // Consultas y arranque
    // ------------------------------------------------------------------

    @Test
    void el_catalogo_indica_que_casos_estan_implementados() {
        ejecutor.implementados = Set.of("TC-SMK-001");

        List<CasoCatalogo> casos = new ListarCatalogoUseCase(catalogo, ejecutor).ejecutar(ADMIN);

        assertEquals(4, casos.size());
        assertTrue(casos.getFirst().implementado());
        assertFalse(casos.get(1).implementado());
    }

    @Test
    void detalle_y_listado_devuelven_lo_guardado() {
        Ejecucion e = lanzarSmoke();

        assertEquals(e.getUuidPublico(),
                new ObtenerDetalleEjecucionUseCase(ejecuciones).ejecutar(ADMIN, e.getUuidPublico()).getUuidPublico());
        assertEquals(1, new ListarEjecucionesUseCase(ejecuciones).ejecutar(ADMIN, new SolicitudPagina(0, 20))
                .totalElementos());
        assertThrows(EjecucionNoEncontradaException.class,
                () -> new ObtenerDetalleEjecucionUseCase(ejecuciones).ejecutar(ADMIN, UUID.randomUUID()));
    }

    @Test
    void al_arrancar_las_activas_quedan_fallidas_por_el_reinicio() {
        Ejecucion e = lanzarSmoke();

        int marcadas = new MarcarInterrumpidasAlArrancarUseCase(ejecuciones, RELOJ).ejecutar();

        assertEquals(1, marcadas);
        Ejecucion despues = ejecuciones.buscarPorUuid(e.getUuidPublico()).orElseThrow();
        assertEquals(EstadoEjecucion.FALLIDA, despues.getEstado());
        assertEquals(MarcarInterrumpidasAlArrancarUseCase.MOTIVO, despues.getMensajeError());
        assertEquals(0, new MarcarInterrumpidasAlArrancarUseCase(ejecuciones, RELOJ).ejecutar());
    }
}
