package bo.phishshield.backend.qa.domain.model;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Traduce entre los enums de la vertical qa y el texto que se guarda en la
 * base. La regla es la misma para todos: en la base va el nombre del enum en
 * minusculas (NOT_RUN se guarda como not_run), igual que en EstadoAlerta.
 */
final class ValorBd {

    private ValorBd() {
    }

    static String de(Enum<?> valor) {
        return valor.name().toLowerCase(Locale.ROOT);
    }

    static <E extends Enum<E>> E convertir(Class<E> tipo, String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Falta el valor de " + campo);
        }
        try {
            return Enum.valueOf(tipo, valor.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            String validos = Arrays.stream(tipo.getEnumConstants())
                    .map(ValorBd::de)
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException(
                    "Valor inválido para " + campo + ": " + valor + " (valores válidos: " + validos + ")");
        }
    }
}
