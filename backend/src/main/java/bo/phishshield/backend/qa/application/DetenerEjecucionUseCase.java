package bo.phishshield.backend.qa.application;

import bo.phishshield.backend.qa.domain.model.Ejecucion;
import bo.phishshield.backend.qa.domain.model.EjecucionNoEncontradaException;
import bo.phishshield.backend.qa.domain.model.EstadoEjecucion;
import bo.phishshield.backend.qa.domain.model.TransicionEjecucionInvalidaException;
import bo.phishshield.backend.qa.domain.port.out.EjecucionRepositoryPort;
import bo.phishshield.backend.qa.domain.port.out.EjecutorPruebasPort;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Caso de uso: detener una ejecucion pendiente o en curso.
 *
 * Si esta corriendo, se le pide al ejecutor que no empiece ningun caso mas
 * (el que esta corriendo termina) y CorrerEjecucionUseCase la deja FALLIDA.
 * Si todavia no empezo, o figura en curso pero nadie la esta corriendo, se
 * marca FALLIDA aqui mismo.
 */
public class DetenerEjecucionUseCase {

    private final EjecucionRepositoryPort ejecuciones;
    private final EjecutorPruebasPort ejecutor;
    private final Clock reloj;

    public DetenerEjecucionUseCase(EjecucionRepositoryPort ejecuciones, EjecutorPruebasPort ejecutor, Clock reloj) {
        this.ejecuciones = ejecuciones;
        this.ejecutor = ejecutor;
        this.reloj = reloj;
    }

    public Ejecucion ejecutar(UsuarioActual quien, UUID id) {
        AlcanceDatos.soloPlataforma(quien);

        Ejecucion ejecucion = ejecuciones.buscarPorUuid(id)
                .orElseThrow(() -> new EjecucionNoEncontradaException(id));
        if (ejecucion.getEstado().esFinal()) {
            throw new TransicionEjecucionInvalidaException(
                    "La ejecución ya terminó (" + ejecucion.getEstado().aValorBd() + "): no hay nada que detener");
        }

        boolean corriendo = ejecutor.detener(id);
        if (!corriendo) {
            String motivo = ejecucion.getEstado() == EstadoEjecucion.PENDIENTE
                    ? CorrerEjecucionUseCase.MOTIVO_DETENIDA + " antes de empezar"
                    : CorrerEjecucionUseCase.MOTIVO_DETENIDA + " (no estaba corriendo en este servidor)";
            ejecucion.fallar(motivo, OffsetDateTime.now(reloj));
            ejecuciones.guardar(ejecucion);
        }

        return ejecuciones.buscarPorUuid(id).orElseThrow(() -> new EjecucionNoEncontradaException(id));
    }
}
