package bo.phishshield.backend.alerta.application;

import bo.phishshield.backend.alerta.domain.model.AlertaFila;
import bo.phishshield.backend.alerta.domain.model.AlertaGestionada;
import bo.phishshield.backend.alerta.domain.model.AlertaModificadaException;
import bo.phishshield.backend.alerta.domain.model.AlertaNoEncontradaException;
import bo.phishshield.backend.alerta.domain.model.EstadoAlerta;
import bo.phishshield.backend.alerta.domain.port.out.AlertaRepositoryPort;
import bo.phishshield.backend.shared.domain.model.AccionAuditoria;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.EventoAuditoria;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import bo.phishshield.backend.shared.domain.port.out.AuditoriaPort;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Caso de uso: cambiar el estado de una alerta y/o sus notas.
 *
 * Coordina; no decide. Si la transicion es valida lo decide
 * AlertaGestionada (dominio). Aca se resuelve el alcance, el orden de los
 * pasos, el guardado con control de concurrencia y la auditoria.
 */
public class ActualizarAlertaUseCase {

    private final AlertaRepositoryPort alertas;
    private final AuditoriaPort auditoria;
    private final Clock reloj;

    public ActualizarAlertaUseCase(AlertaRepositoryPort alertas, AuditoriaPort auditoria, Clock reloj) {
        this.alertas = alertas;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    public AlertaFila ejecutar(UsuarioActual quien, ComandoActualizarAlerta comando) {
        AlcanceDatos alcance = AlcanceDatos.administrativo(quien);

        AlertaGestionada alerta = alertas.buscarPorUuid(comando.id())
                .filter(a -> alcance.incluye(a.getIdEmpresa()))
                .orElseThrow(() -> new AlertaNoEncontradaException(comando.id()));

        EstadoAlerta estadoLeido = alerta.getEstado();
        String notasAntes = alerta.getNotas();

        // Primero las notas: asi "descartar con una nota" funciona en un solo
        // paso (despues de cerrarla ya no se podria anotar).
        if (comando.notas() != null) {
            alerta.anotar(comando.notas());
        }
        // Pedir el mismo estado que ya tiene no es un error: no cambia nada.
        if (comando.estadoNuevo() != null && comando.estadoNuevo() != estadoLeido) {
            alerta.cambiarEstado(comando.estadoNuevo(), quien.idUsuario(), OffsetDateTime.now(reloj));
        }

        if (!alertas.actualizar(alerta, estadoLeido)) {
            throw new AlertaModificadaException();
        }

        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("alerta", alerta.getUuidPublico().toString());
        detalle.put("estado_anterior", estadoLeido.aValorBd());
        detalle.put("estado_nuevo", alerta.getEstado().aValorBd());
        detalle.put("notas_cambiadas", !Objects.equals(notasAntes, alerta.getNotas()));
        auditoria.registrar(EventoAuditoria.de(AccionAuditoria.ALERTA_ACTUALIZADA,
                quien, "alertas", alerta.getId(), detalle, comando.ip()));

        return alertas.buscarFila(comando.id())
                .orElseThrow(() -> new AlertaNoEncontradaException(comando.id()));
    }
}
