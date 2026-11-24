package bo.phishshield.backend.qa.application;

import bo.phishshield.backend.qa.dobles.DoblesQa;
import bo.phishshield.backend.qa.dobles.DoblesQa.EjecucionesEnMemoria;
import bo.phishshield.backend.qa.domain.model.AlcanceEjecucion;
import bo.phishshield.backend.qa.domain.model.AnalisisFallo;
import bo.phishshield.backend.qa.domain.model.AsistenteNoDisponibleException;
import bo.phishshield.backend.qa.domain.model.ContextoFallo;
import bo.phishshield.backend.qa.domain.model.CorreoDestino;
import bo.phishshield.backend.qa.domain.model.CorreoReporte;
import bo.phishshield.backend.qa.domain.model.DesenlaceCaso;
import bo.phishshield.backend.qa.domain.model.Ejecucion;
import bo.phishshield.backend.qa.domain.model.EstadoCaso;
import bo.phishshield.backend.qa.domain.model.SeleccionInvalidaException;
import bo.phishshield.backend.qa.domain.model.Suite;
import bo.phishshield.backend.qa.domain.model.TipoFallo;
import bo.phishshield.backend.qa.domain.port.out.AsistenteIAPort;
import bo.phishshield.backend.qa.domain.port.out.NotificadorCorreoPort;
import bo.phishshield.backend.shared.domain.model.AccesoDenegadoException;
import bo.phishshield.backend.shared.domain.model.Rol;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** El asistente de IA solo explica: nunca cambia el estado de los casos. */
class AsistenteQaUseCaseTest {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-24T10:00:00-04:00");
    private static final UsuarioActual ADMIN = new UsuarioActual(1L, UUID.randomUUID(), "a@phishshield.bo", null, Rol.ADMIN);

    private final EjecucionesEnMemoria ejecuciones = new EjecucionesEnMemoria();
    private final List<ContextoFallo> consultados = new ArrayList<>();
    private boolean disponible = true;
    private boolean responde = true;

    private final AsistenteIAPort asistente = new AsistenteIAPort() {
        @Override
        public boolean estaDisponible() {
            return disponible;
        }

        @Override
        public Optional<AnalisisFallo> analizarFallo(ContextoFallo contexto) {
            consultados.add(contexto);
            return responde ? Optional.of(new AnalisisFallo("Falta BancoSol en las entidades", "Paso 1")) : Optional.empty();
        }

        @Override
        public Optional<String> resumirEjecucion(Ejecucion ejecucion) {
            return responde ? Optional.of("Resumen") : Optional.empty();
        }
    };

    private final NotificadorCorreoPort sinCorreo = new NotificadorCorreoPort() {
        @Override
        public boolean estaConfigurado() {
            return false;
        }

        @Override
        public void enviar(CorreoReporte correo) {
            fail("No debía enviar");
        }
    };

    private final AsistenteQaUseCase casoDeUso = new AsistenteQaUseCase(ejecuciones, asistente, sinCorreo);

    private Ejecucion terminadaConUnFail() {
        Ejecucion e = ejecuciones.guardar(Ejecucion.nueva(1L, Suite.SANITY, AlcanceEjecucion.SUITE_COMPLETA,
                new CorreoDestino("a@phishshield.bo"), "qa", Map.of(), List.of(
                        DoblesQa.caso("TC-SAN-001", Suite.SANITY, 1), DoblesQa.caso("TC-SAN-003", Suite.SANITY, 3)), AHORA));
        e.iniciar(AHORA);
        e.registrarResultado(DesenlaceCaso.aprobado("TC-SAN-001", AHORA, AHORA, List.of(), List.of()));
        e.registrarResultado(DesenlaceCaso.fallido("TC-SAN-003", TipoFallo.FUNCIONAL, "expected phishing", "Verificar",
                AHORA, AHORA, List.of(), List.of()));
        e.finalizar(AHORA);
        ejecuciones.guardar(e);
        return e;
    }

    @Test
    void informa_las_capacidades_opcionales() {
        assertTrue(casoDeUso.capacidades(ADMIN).asistenteDisponible());
        assertFalse(casoDeUso.capacidades(ADMIN).correoConfigurado());
    }

    @Test
    void analiza_un_fail_sin_tocar_su_estado() {
        Ejecucion e = terminadaConUnFail();

        AnalisisFallo analisis = casoDeUso.analizarFallo(ADMIN, e.getUuidPublico(), "tc-san-003");

        assertEquals("Falta BancoSol en las entidades", analisis.causaProbable());
        assertEquals("TC-SAN-003", consultados.getFirst().codigo());
        assertEquals(EstadoCaso.FAIL, ejecuciones.buscarPorUuid(e.getUuidPublico()).orElseThrow()
                .getResultados().get(1).getEstado(), "El asistente no cambia el estado");
    }

    @Test
    void solo_analiza_casos_en_fail() {
        Ejecucion e = terminadaConUnFail();

        assertThrows(SeleccionInvalidaException.class, () -> casoDeUso.analizarFallo(ADMIN, e.getUuidPublico(), "TC-SAN-001"));
        assertThrows(SeleccionInvalidaException.class, () -> casoDeUso.analizarFallo(ADMIN, e.getUuidPublico(), "TC-SAN-099"));
        assertTrue(consultados.isEmpty());
    }

    @Test
    void sin_asistente_o_sin_respuesta_avisa_que_no_esta_disponible() {
        Ejecucion e = terminadaConUnFail();

        responde = false;
        assertThrows(AsistenteNoDisponibleException.class, () -> casoDeUso.resumir(ADMIN, e.getUuidPublico()));

        disponible = false;
        AsistenteNoDisponibleException ex = assertThrows(AsistenteNoDisponibleException.class,
                () -> casoDeUso.analizarFallo(ADMIN, e.getUuidPublico(), "TC-SAN-003"));
        assertTrue(ex.getMessage().contains("ANTHROPIC_API_KEY"));
    }

    @Test
    void solo_el_admin_usa_el_asistente() {
        UsuarioActual empresa = new UsuarioActual(2L, UUID.randomUUID(), "m@andina.bo", 1L, Rol.ADMIN_EMPRESA);
        assertThrows(AccesoDenegadoException.class, () -> casoDeUso.capacidades(empresa));
        assertThrows(AccesoDenegadoException.class, () -> casoDeUso.resumir(empresa, UUID.randomUUID()));
    }
}
