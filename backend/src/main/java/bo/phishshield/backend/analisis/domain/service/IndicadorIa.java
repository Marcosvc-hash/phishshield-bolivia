package bo.phishshield.backend.analisis.domain.service;

import bo.phishshield.backend.analisis.domain.model.Indicador;
import bo.phishshield.backend.analisis.domain.model.PrediccionIa;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * IA_MODELO_URL: convierte la prediccion del modelo de IA en un indicador.
 *
 * Solo aporta si la probabilidad alcanza el umbral del propio modelo; por
 * debajo, la IA no suma nada (no resta: un modelo no puede "limpiar" lo que
 * encontraron las reglas). El aporte es proporcional a la confianza:
 * probabilidad x peso, con el peso leido de reglas_deteccion.
 */
public final class IndicadorIa {

    public static final String CODIGO = "IA_MODELO_URL";

    private static final int MAX_SENALES = 3;
    private static final int LARGO_MAXIMO = 255;   // analisis_indicadores.valor es VARCHAR(255)

    private IndicadorIa() {
    }

    public static Optional<Indicador> desde(PrediccionIa prediccion, BigDecimal peso) {
        if (!prediccion.superaUmbral()) {
            return Optional.empty();
        }
        BigDecimal aporte = prediccion.probabilidad()
                .multiply(peso == null ? BigDecimal.ZERO : peso)
                .setScale(2, RoundingMode.HALF_UP);
        return Optional.of(new Indicador(CODIGO, evidencia(prediccion), aporte));
    }

    /**
     * "Modelo de IA 1.0.0: 97% de probabilidad de phishing. Senales: a; b; c".
     * Si no entra en 255 caracteres, se quitan senales desde la mas debil.
     */
    static String evidencia(PrediccionIa p) {
        int porcentaje = p.probabilidad().movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValue();
        String base = "Modelo de IA " + p.version() + ": " + porcentaje + "% de probabilidad de phishing";

        List<String> senales = p.senales().stream().limit(MAX_SENALES).toList();
        for (int n = senales.size(); n > 0; n--) {
            String texto = base + ". Senales: " + String.join("; ", senales.subList(0, n));
            if (texto.length() <= LARGO_MAXIMO) {
                return texto;
            }
        }
        return base;
    }
}
