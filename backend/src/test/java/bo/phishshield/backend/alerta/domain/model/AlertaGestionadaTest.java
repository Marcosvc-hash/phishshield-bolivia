package bo.phishshield.backend.alerta.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AlertaGestionadaTest {

    private static final OffsetDateTime NOTIFICADA = OffsetDateTime.parse("2026-11-09T10:00:00-04:00");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-09T11:30:00-04:00");
    private static final Long MARIA = 7L;

    private static AlertaGestionada en(EstadoAlerta estado) {
        return new AlertaGestionada(1L, UUID.randomUUID(), 3L, SeveridadAlerta.ALTA, estado,
                null, null, NOTIFICADA, null);
    }

    /** La tabla completa de transiciones: 4 x 4 combinaciones. */
    @ParameterizedTest(name = "{0} -> {1}: {2}")
    @CsvSource({
            "PENDIENTE,   EN_REVISION, true",
            "PENDIENTE,   REVISADA,    false",
            "PENDIENTE,   DESCARTADA,  false",
            "PENDIENTE,   PENDIENTE,   false",
            "EN_REVISION, REVISADA,    true",
            "EN_REVISION, DESCARTADA,  true",
            "EN_REVISION, PENDIENTE,   false",
            "EN_REVISION, EN_REVISION, false",
            "REVISADA,    DESCARTADA,  false",
            "REVISADA,    EN_REVISION, false",
            "REVISADA,    PENDIENTE,   false",
            "DESCARTADA,  REVISADA,    false",
            "DESCARTADA,  EN_REVISION, false",
            "DESCARTADA,  PENDIENTE,   false",
    })
    void solo_se_permiten_las_transiciones_del_ciclo_de_vida(EstadoAlerta desde, EstadoAlerta hacia, boolean valida) {
        assertEquals(valida, desde.puedePasarA(hacia));

        AlertaGestionada alerta = en(desde);
        if (valida) {
            alerta.cambiarEstado(hacia, MARIA, AHORA);
            assertEquals(hacia, alerta.getEstado());
        } else {
            assertThrows(TransicionAlertaInvalidaException.class, () -> alerta.cambiarEstado(hacia, MARIA, AHORA));
            assertEquals(desde, alerta.getEstado(), "Una transicion rechazada no cambia nada");
        }
    }

    @Test
    void tomarla_en_revision_la_asigna_a_quien_la_tomo() {
        AlertaGestionada alerta = en(EstadoAlerta.PENDIENTE);

        alerta.cambiarEstado(EstadoAlerta.EN_REVISION, MARIA, AHORA);

        assertEquals(MARIA, alerta.getIdAsignado());
        assertNull(alerta.getFechaCierre());
    }

    @Test
    void cerrarla_guarda_la_fecha_de_cierre() {
        AlertaGestionada alerta = en(EstadoAlerta.EN_REVISION);

        alerta.cambiarEstado(EstadoAlerta.DESCARTADA, MARIA, AHORA);

        assertEquals(AHORA, alerta.getFechaCierre());
    }

    @Test
    void la_fecha_de_cierre_nunca_queda_antes_de_la_notificacion() {
        AlertaGestionada alerta = en(EstadoAlerta.EN_REVISION);

        alerta.cambiarEstado(EstadoAlerta.REVISADA, MARIA, NOTIFICADA.minusMinutes(2));

        assertEquals(NOTIFICADA, alerta.getFechaCierre(), "chk_alertas_cierre lo exige");
    }

    @Test
    void las_notas_se_limpian_y_vacias_se_borran() {
        AlertaGestionada alerta = en(EstadoAlerta.EN_REVISION);

        alerta.anotar("  Llamamos al usuario  ");
        assertEquals("Llamamos al usuario", alerta.getNotas());

        alerta.anotar("   ");
        assertNull(alerta.getNotas());
    }

    @Test
    void una_alerta_cerrada_no_acepta_notas() {
        AlertaGestionada alerta = en(EstadoAlerta.REVISADA);

        assertThrows(TransicionAlertaInvalidaException.class, () -> alerta.anotar("tarde"));
    }

    @Test
    void notas_demasiado_largas_son_rechazadas() {
        AlertaGestionada alerta = en(EstadoAlerta.PENDIENTE);

        assertThrows(IllegalArgumentException.class,
                () -> alerta.anotar("x".repeat(AlertaGestionada.MAXIMO_NOTAS + 1)));
    }

    @Test
    void los_textos_de_la_base_se_traducen_y_los_invalidos_se_rechazan() {
        assertEquals(EstadoAlerta.EN_REVISION, EstadoAlerta.desdeValorBd("en_revision"));
        assertEquals("en_revision", EstadoAlerta.EN_REVISION.aValorBd());
        assertEquals(SeveridadAlerta.BAJA, SeveridadAlerta.desdeValorBd("baja"));
        assertThrows(IllegalArgumentException.class, () -> EstadoAlerta.desdeValorBd("cerrada"));
        assertTrue(EstadoAlerta.REVISADA.esFinal());
        assertFalse(EstadoAlerta.EN_REVISION.esFinal());
    }
}
