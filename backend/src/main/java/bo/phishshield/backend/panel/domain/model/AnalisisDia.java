package bo.phishshield.backend.panel.domain.model;

import java.time.LocalDate;

/** Una barra del grafico: cuantos analisis hubo un dia, por veredicto. */
public record AnalisisDia(LocalDate dia,
                          long total,
                          long phishing,
                          long sospechosos,
                          long seguros) {

    public static AnalisisDia vacio(LocalDate dia) {
        return new AnalisisDia(dia, 0, 0, 0, 0);
    }
}
