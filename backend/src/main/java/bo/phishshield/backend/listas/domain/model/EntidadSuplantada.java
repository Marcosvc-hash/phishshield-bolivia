package bo.phishshield.backend.listas.domain.model;

/**
 * Una marca boliviana usada como cebo (entidades_suplantadas). La tabla no
 * tiene uuid_publico, pero el nombre es UNIQUE: la API identifica a la
 * entidad por su nombre y el BIGINT no sale.
 */
public record EntidadSuplantada(String nombre, String tipo, String dominioOficial) {
}
