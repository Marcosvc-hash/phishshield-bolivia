package bo.phishshield.backend.qa.application;

import bo.phishshield.backend.qa.domain.model.Ejecucion;
import bo.phishshield.backend.qa.domain.port.out.EjecucionRepositoryPort;

import java.time.Clock;
import java.time.OffsetDateTime;

/**
 * Caso de uso: al arrancar el backend, ninguna ejecucion puede estar
 * corriendo. Las que figuran pendientes o en curso quedaron interrumpidas
 * por un reinicio: se marcan FALLIDA para que no bloqueen las nuevas (solo
 * se admite una activa a la vez).
 */
public class MarcarInterrumpidasAlArrancarUseCase {

    static final String MOTIVO = "Interrumpida: el backend se reinició mientras la ejecución estaba activa";

    private final EjecucionRepositoryPort ejecuciones;
    private final Clock reloj;

    public MarcarInterrumpidasAlArrancarUseCase(EjecucionRepositoryPort ejecuciones, Clock reloj) {
        this.ejecuciones = ejecuciones;
        this.reloj = reloj;
    }

    /** @return cuantas ejecuciones se marcaron */
    public int ejecutar() {
        int marcadas = 0;
        for (Ejecucion ejecucion : ejecuciones.listarActivas()) {
            ejecucion.fallar(MOTIVO, OffsetDateTime.now(reloj));
            ejecuciones.guardar(ejecucion);
            marcadas++;
        }
        return marcadas;
    }
}
