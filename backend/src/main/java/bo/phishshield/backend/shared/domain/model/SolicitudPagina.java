package bo.phishshield.backend.shared.domain.model;

/**
 * Que pagina de resultados se pide. La pagina empieza en 0, como en Spring Data.
 * Los valores fuera de rango se corrigen en vez de fallar: una pagina negativa
 * pasa a 0 y un tamano enorme queda en el maximo, para que nadie pueda pedir
 * la tabla entera de un golpe.
 */
public record SolicitudPagina(int pagina, int tamano) {

    public static final int TAMANO_POR_DEFECTO = 20;
    public static final int TAMANO_MAXIMO = 100;

    public SolicitudPagina {
        pagina = Math.max(0, pagina);
        tamano = tamano < 1 ? TAMANO_POR_DEFECTO : Math.min(tamano, TAMANO_MAXIMO);
    }

    /** Cuantas filas saltar en el SQL (OFFSET). */
    public long desplazamiento() {
        return (long) pagina * tamano;
    }
}
