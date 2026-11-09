package bo.phishshield.backend.panel.domain.model;

/** Una entidad boliviana y cuantas veces la suplantaron (banco, billetera, gobierno...). */
public record EntidadTop(String entidad, String tipo, long veces) {
}
