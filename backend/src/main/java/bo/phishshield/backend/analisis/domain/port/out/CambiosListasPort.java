package bo.phishshield.backend.analisis.domain.port.out;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Puerto de salida: cuando cambio por ultima vez una lista de dominios
 * (alta, edicion, baja o reactivacion). Sirve para no reutilizar un
 * analisis guardado que se calculo con las listas viejas.
 */
public interface CambiosListasPort {

    /** Vacio si nunca hubo cambios desde el panel. */
    Optional<OffsetDateTime> ultimoCambio();
}
