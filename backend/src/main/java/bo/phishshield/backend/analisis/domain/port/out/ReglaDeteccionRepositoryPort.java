package bo.phishshield.backend.analisis.domain.port.out;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Puerto de salida: pesos de las reglas activas.
 * El dominio necesita saber cuanto vale cada regla, pero no sabe
 * que existe una tabla reglas_deteccion ni que hay PostgreSQL detras.
 */
public interface ReglaDeteccionRepositoryPort {

    /**
     * Pesos de las reglas activas de una categoria, indexados por codigo.
     * @param categoria url | contenido | remitente | adjunto | visual
     */
    Map<String, BigDecimal> pesosActivosPorCategoria(String categoria);
}