package bo.phishshield.backend.historial.domain.model;

import bo.phishshield.backend.analisis.domain.model.Resultado;
import bo.phishshield.backend.shared.domain.model.RangoFechas;

/**
 * Filtros del historial. Todos son opcionales.
 *
 * @param resultado seguro, sospechoso o phishing; null = todos
 * @param texto     se busca dentro del dominio y de la URL completa; null = sin filtro
 */
public record FiltroHistorial(Resultado resultado, RangoFechas fechas, String texto) {

    public FiltroHistorial {
        if (fechas == null) {
            fechas = RangoFechas.sinLimites();
        }
        texto = texto == null || texto.isBlank() ? null : texto.trim();
    }

    /** Convierte el texto de la peticion ("phishing") en el enum; vacio = sin filtro. */
    public static Resultado resultadoDesdeTexto(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Resultado.desdeValorBd(valor.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Resultado invalido: " + valor
                    + " (use seguro, sospechoso o phishing)");
        }
    }
}
