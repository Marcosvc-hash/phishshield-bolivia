package bo.phishshield.backend.listas.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Una fila de la pagina de listas, con el nombre de la entidad suplantada. */
public record DominioFila(UUID id,
                          String dominio,
                          String lista,
                          String entidad,
                          String fuente,
                          String motivo,
                          boolean activo,
                          OffsetDateTime creadoEn) {
}
