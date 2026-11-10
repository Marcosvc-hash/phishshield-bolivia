package bo.phishshield.backend.alerta.application;

import bo.phishshield.backend.alerta.domain.model.AlertaFila;
import bo.phishshield.backend.alerta.domain.model.AlertaGestionada;
import bo.phishshield.backend.alerta.domain.model.AlertaModificadaException;
import bo.phishshield.backend.alerta.domain.model.AlertaNoEncontradaException;
import bo.phishshield.backend.alerta.domain.model.EstadoAlerta;
import bo.phishshield.backend.alerta.domain.model.FiltroAlertas;
import bo.phishshield.backend.alerta.domain.model.SeveridadAlerta;
import bo.phishshield.backend.alerta.domain.model.TransicionAlertaInvalidaException;
import bo.phishshield.backend.alerta.domain.port.out.AlertaRepositoryPort;
import bo.phishshield.backend.shared.dobles.AuditoriaEnMemoria;
import bo.phishshield.backend.shared.domain.model.AccesoDenegadoException;
import bo.phishshield.backend.shared.domain.model.AccionAuditoria;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.Pagina;
import bo.phishshield.backend.shared.domain.model.Rol;
import bo.phishshield.backend.shared.domain.model.SolicitudPagina;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ActualizarAlertaUseCaseTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-09T15:00:00Z"), ZoneOffset.ofHours(-4));
    private static final long ANDINA = 1L;
    private static final long OTRA = 2L;
    private static final String IP = "190.129.10.25";

    /**
     * Doble en memoria que imita al adaptador, incluido el control
     * optimista: solo guarda si la alerta "en la base" sigue en estadoLeido.
     */
    static class AlertasEnMemoria implements AlertaRepositoryPort {
        final Map<UUID, AlertaGestionada> enBase = new HashMap<>();
        /** Si se pone, simula que otro admin la cambio a este estado justo antes de guardar. */
        EstadoAlerta cambioDeOtro;

        @Override
        public Pagina<AlertaFila> buscar(AlcanceDatos alcance, FiltroAlertas filtro, SolicitudPagina pagina) {
            return Pagina.de(List.of(), pagina, 0);
        }

        @Override
        public long contarPendientes(AlcanceDatos alcance) {
            return enBase.values().stream()
                    .filter(a -> a.getEstado() == EstadoAlerta.PENDIENTE && alcance.incluye(a.getIdEmpresa()))
                    .count();
        }

        /** Devuelve una copia: el caso de uso no debe poder cambiar "la base" sin guardar. */
        @Override
        public Optional<AlertaGestionada> buscarPorUuid(UUID id) {
            return Optional.ofNullable(enBase.get(id)).map(AlertasEnMemoria::copia);
        }

        @Override
        public Optional<AlertaFila> buscarFila(UUID id) {
            AlertaGestionada a = enBase.get(id);
            return Optional.ofNullable(a).map(x -> new AlertaFila(x.getUuidPublico(), UUID.randomUUID(),
                    x.getSeveridad().aValorBd(), x.getEstado().aValorBd(), "http://x.xyz", "x.xyz", null,
                    null, null, null, x.getNotas(), x.getFechaNotificacion(), x.getFechaCierre()));
        }

        @Override
        public boolean actualizar(AlertaGestionada alerta, EstadoAlerta estadoLeido) {
            EstadoAlerta actual = cambioDeOtro != null ? cambioDeOtro : enBase.get(alerta.getUuidPublico()).getEstado();
            if (actual != estadoLeido) {
                return false;
            }
            enBase.put(alerta.getUuidPublico(), copia(alerta));
            return true;
        }

        UUID agregar(Long idEmpresa, EstadoAlerta estado) {
            UUID id = UUID.randomUUID();
            enBase.put(id, new AlertaGestionada((long) enBase.size() + 1, id, idEmpresa, SeveridadAlerta.ALTA,
                    estado, null, null, OffsetDateTime.parse("2026-11-09T09:00:00-04:00"), null));
            return id;
        }

        private static AlertaGestionada copia(AlertaGestionada a) {
            return new AlertaGestionada(a.getId(), a.getUuidPublico(), a.getIdEmpresa(), a.getSeveridad(),
                    a.getEstado(), a.getIdAsignado(), a.getNotas(), a.getFechaNotificacion(), a.getFechaCierre());
        }
    }

    private final AlertasEnMemoria alertas = new AlertasEnMemoria();
    private final AuditoriaEnMemoria auditoria = new AuditoriaEnMemoria();
    private final ActualizarAlertaUseCase casoDeUso = new ActualizarAlertaUseCase(alertas, auditoria, RELOJ);
    private final ListarAlertasUseCase listar = new ListarAlertasUseCase(alertas);

    private static final UsuarioActual MARIA = new UsuarioActual(7L, UUID.randomUUID(), "maria@andina.bo", ANDINA, Rol.ADMIN_EMPRESA);
    private static final UsuarioActual ADMIN = new UsuarioActual(1L, UUID.randomUUID(), "admin@x.bo", null, Rol.ADMIN);

    private static ComandoActualizarAlerta comando(UUID id, EstadoAlerta estado, String notas) {
        return new ComandoActualizarAlerta(id, estado, notas, IP);
    }

    @Test
    void recorrido_completo_pendiente_en_revision_revisada() {
        UUID id = alertas.agregar(ANDINA, EstadoAlerta.PENDIENTE);

        casoDeUso.ejecutar(MARIA, comando(id, EstadoAlerta.EN_REVISION, null));
        assertEquals(EstadoAlerta.EN_REVISION, alertas.enBase.get(id).getEstado());
        assertEquals(MARIA.idUsuario(), alertas.enBase.get(id).getIdAsignado());

        AlertaFila fila = casoDeUso.ejecutar(MARIA, comando(id, EstadoAlerta.REVISADA, "Era phishing real; se aviso al banco"));
        assertEquals("revisada", fila.estado());
        assertEquals("Era phishing real; se aviso al banco", fila.notas());
        assertEquals(OffsetDateTime.now(RELOJ), alertas.enBase.get(id).getFechaCierre());
    }

    @Test
    void descartar_con_nota_en_un_solo_paso_guarda_la_nota() {
        UUID id = alertas.agregar(ANDINA, EstadoAlerta.EN_REVISION);

        casoDeUso.ejecutar(MARIA, comando(id, EstadoAlerta.DESCARTADA, "Falso positivo"));

        assertEquals("Falso positivo", alertas.enBase.get(id).getNotas());
        assertEquals(EstadoAlerta.DESCARTADA, alertas.enBase.get(id).getEstado());
    }

    @Test
    void saltarse_la_revision_es_rechazado_y_no_guarda_nada() {
        UUID id = alertas.agregar(ANDINA, EstadoAlerta.PENDIENTE);

        assertThrows(TransicionAlertaInvalidaException.class,
                () -> casoDeUso.ejecutar(MARIA, comando(id, EstadoAlerta.REVISADA, "nota")));

        assertEquals(EstadoAlerta.PENDIENTE, alertas.enBase.get(id).getEstado());
        assertNull(alertas.enBase.get(id).getNotas());
        assertTrue(auditoria.eventos.isEmpty());
    }

    @Test
    void una_alerta_cerrada_no_se_reabre() {
        UUID id = alertas.agregar(ANDINA, EstadoAlerta.DESCARTADA);

        assertThrows(TransicionAlertaInvalidaException.class,
                () -> casoDeUso.ejecutar(ADMIN, comando(id, EstadoAlerta.EN_REVISION, null)));
    }

    @Test
    void el_admin_de_empresa_no_toca_alertas_de_otra_empresa_ni_anonimas() {
        UUID deOtra = alertas.agregar(OTRA, EstadoAlerta.PENDIENTE);
        UUID anonima = alertas.agregar(null, EstadoAlerta.PENDIENTE);

        assertThrows(AlertaNoEncontradaException.class,
                () -> casoDeUso.ejecutar(MARIA, comando(deOtra, EstadoAlerta.EN_REVISION, null)));
        assertThrows(AlertaNoEncontradaException.class,
                () -> casoDeUso.ejecutar(MARIA, comando(anonima, EstadoAlerta.EN_REVISION, null)));

        // El ADMIN si puede
        casoDeUso.ejecutar(ADMIN, comando(anonima, EstadoAlerta.EN_REVISION, null));
        assertEquals(EstadoAlerta.EN_REVISION, alertas.enBase.get(anonima).getEstado());
    }

    @Test
    void un_usuario_comun_no_gestiona_alertas() {
        UUID id = alertas.agregar(ANDINA, EstadoAlerta.PENDIENTE);
        UsuarioActual carlos = new UsuarioActual(9L, UUID.randomUUID(), "c@andina.bo", ANDINA, Rol.USUARIO);

        assertThrows(AccesoDenegadoException.class,
                () -> casoDeUso.ejecutar(carlos, comando(id, EstadoAlerta.EN_REVISION, null)));
    }

    @Test
    void si_otro_admin_la_cambio_antes_se_rechaza_sin_pisar_su_cambio() {
        UUID id = alertas.agregar(ANDINA, EstadoAlerta.PENDIENTE);
        alertas.cambioDeOtro = EstadoAlerta.EN_REVISION;

        assertThrows(AlertaModificadaException.class,
                () -> casoDeUso.ejecutar(MARIA, comando(id, EstadoAlerta.EN_REVISION, "yo tambien")));
        assertNull(alertas.enBase.get(id).getNotas());
    }

    @Test
    void cada_cambio_queda_en_auditoria() {
        UUID id = alertas.agregar(ANDINA, EstadoAlerta.PENDIENTE);

        casoDeUso.ejecutar(MARIA, comando(id, EstadoAlerta.EN_REVISION, "Revisando"));

        assertEquals(List.of(AccionAuditoria.ALERTA_ACTUALIZADA), auditoria.acciones());
        Map<String, Object> detalle = auditoria.ultimo().detalle();
        assertEquals("pendiente", detalle.get("estado_anterior"));
        assertEquals("en_revision", detalle.get("estado_nuevo"));
        assertEquals(true, detalle.get("notas_cambiadas"));
        assertEquals(IP, auditoria.ultimo().ip());
    }

    @Test
    void sin_estado_ni_notas_el_comando_es_invalido() {
        assertThrows(IllegalArgumentException.class, () -> comando(UUID.randomUUID(), null, null));
    }

    @Test
    void el_contador_cuenta_solo_las_pendientes_del_alcance() {
        alertas.agregar(ANDINA, EstadoAlerta.PENDIENTE);
        alertas.agregar(ANDINA, EstadoAlerta.EN_REVISION);
        alertas.agregar(OTRA, EstadoAlerta.PENDIENTE);
        alertas.agregar(null, EstadoAlerta.PENDIENTE);

        assertEquals(1, listar.contarPendientes(MARIA));
        assertEquals(3, listar.contarPendientes(ADMIN));
    }
}
