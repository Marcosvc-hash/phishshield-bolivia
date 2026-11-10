package bo.phishshield.backend.listas.domain.model;

/**
 * El cambio choca con lo que ya hay en las listas: el dominio ya esta
 * activo en esa lista, o esta activo en la lista opuesta (un dominio no
 * puede ser legitimo y malicioso a la vez). La web responde 409.
 */
public class ConflictoListaException extends RuntimeException {

    public ConflictoListaException(String mensaje) {
        super(mensaje);
    }
}
