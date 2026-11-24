package bo.phishshield.backend.reporte.application;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Pedido de reporte.
 *
 * @param idEmpresa uuid publico de la empresa; el ADMIN_EMPRESA puede omitirlo (se usa la suya)
 * @param ip        de quien lo pide, para la auditoria
 */
public record ComandoGenerarReporte(UUID idEmpresa, LocalDate desde, LocalDate hasta, String ip) {
}
