package bo.phishshield.backend.listas.domain.model;

/**
 * Filtros de la pagina de listas; null = sin filtro.
 *
 * @param texto parte del dominio o del motivo
 */
public record FiltroDominios(TipoLista lista, Boolean activo, String texto) {

    public FiltroDominios {
        texto = texto == null || texto.isBlank() ? null : texto.trim().toLowerCase();
    }

    public static FiltroDominios desdeTexto(String lista, Boolean activo, String texto) {
        return new FiltroDominios(
                lista == null || lista.isBlank() ? null : TipoLista.desdeValorBd(lista), activo, texto);
    }
}
