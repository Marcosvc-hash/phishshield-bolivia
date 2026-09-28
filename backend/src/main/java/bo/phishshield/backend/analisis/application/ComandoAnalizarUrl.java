package bo.phishshield.backend.analisis.application;

import bo.phishshield.backend.analisis.domain.model.OrigenAnalisis;

/**
 * Entrada del caso de uso. Existe para que el caso de uso no dependa
 * de un DTO web: manana podria llegar desde la extension de navegador
 * o desde un job por lotes, sin cambiar nada aca.
 */
public record ComandoAnalizarUrl(String url,
                                 OrigenAnalisis origen,
                                 Long idUsuario,
                                 Long idEmpresa) {

    public ComandoAnalizarUrl {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("La URL es obligatoria");
        }
        if (origen == null) {
            origen = OrigenAnalisis.WEB;
        }
    }
}