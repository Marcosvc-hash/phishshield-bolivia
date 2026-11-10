package bo.phishshield.backend.shared.domain.model;

import java.util.List;
import java.util.function.Function;

/**
 * Una pagina de resultados y el total de filas que cumplen el filtro.
 * Equivale a un PagedResult&lt;T&gt; de C#.
 */
public record Pagina<T>(List<T> contenido, int pagina, int tamano, long totalElementos) {

    public Pagina {
        contenido = List.copyOf(contenido);
    }

    public static <T> Pagina<T> de(List<T> contenido, SolicitudPagina solicitud, long totalElementos) {
        return new Pagina<>(contenido, solicitud.pagina(), solicitud.tamano(), totalElementos);
    }

    public int totalPaginas() {
        return tamano == 0 ? 0 : (int) ((totalElementos + tamano - 1) / tamano);
    }

    /** La misma pagina con cada elemento convertido (por ejemplo, a un DTO). */
    public <R> Pagina<R> map(Function<T, R> conversion) {
        return new Pagina<>(contenido.stream().map(conversion).toList(), pagina, tamano, totalElementos);
    }
}
