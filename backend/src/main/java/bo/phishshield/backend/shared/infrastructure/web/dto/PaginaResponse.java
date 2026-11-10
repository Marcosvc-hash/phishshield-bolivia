package bo.phishshield.backend.shared.infrastructure.web.dto;

import bo.phishshield.backend.shared.domain.model.Pagina;

import java.util.List;
import java.util.function.Function;

/** Forma JSON comun de todas las respuestas paginadas de administracion. */
public record PaginaResponse<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas
) {

    public static <D, T> PaginaResponse<T> desde(Pagina<D> pagina, Function<D, T> conversion) {
        Pagina<T> convertida = pagina.map(conversion);
        return new PaginaResponse<>(
                convertida.contenido(),
                convertida.pagina(),
                convertida.tamano(),
                convertida.totalElementos(),
                convertida.totalPaginas());
    }
}
