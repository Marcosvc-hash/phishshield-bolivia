package bo.phishshield.backend.analisis.domain.service;

import bo.phishshield.backend.analisis.domain.model.UrlAnalizada;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * URL_RUTA_SENSIBLE: la ruta pide credenciales o datos de la cuenta
 * ("/banca/login", "/verificar-cuenta"). Sola suma poco; sirve para
 * reforzar otras senales, como una IP literal sin HTTPS.
 *
 * Compara palabras completas, sin distinguir mayusculas ni tildes:
 * "/Login.php" y "/contraseña" cuentan, "/blogin-info" no.
 * Solo mira la ruta, no el host ni los parametros (?...).
 */
public class ReglaRutaSensible implements ReglaUrl {

    private static final List<String> PALABRAS = List.of(
            "login", "signin", "verificar", "verificacion", "cuenta", "banca",
            "password", "contrasena", "clave", "pin", "token", "actualizar-datos");

    private static final Pattern NO_ALFANUMERICO = Pattern.compile("[^a-z0-9]+");
    private static final Pattern MARCAS_DIACRITICAS = Pattern.compile("\\p{M}");

    @Override
    public String codigo() {
        return "URL_RUTA_SENSIBLE";
    }

    @Override
    public Optional<String> evaluar(UrlAnalizada url) {
        Set<String> encontradas = new LinkedHashSet<>();

        for (String segmento : normalizar(url.getRuta()).split("/")) {
            // "verificar-cuenta.php" queda como "-verificar-cuenta-php-": asi
            // se buscan palabras completas, incluida la compuesta "actualizar-datos"
            String palabras = "-" + String.join("-", Arrays.stream(NO_ALFANUMERICO.split(segmento))
                    .filter(p -> !p.isEmpty())
                    .toList()) + "-";

            for (String palabra : PALABRAS) {
                if (palabras.contains("-" + palabra + "-")) {
                    encontradas.add(palabra);
                }
            }
        }

        if (encontradas.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of("La ruta contiene palabras sensibles: " + String.join(", ", encontradas));
    }

    /** Minusculas y sin tildes: "/Contraseña" devuelve "/contrasena". */
    private String normalizar(String ruta) {
        String sinTildes = MARCAS_DIACRITICAS
                .matcher(Normalizer.normalize(ruta, Normalizer.Form.NFD))
                .replaceAll("");
        return sinTildes.toLowerCase(Locale.ROOT);
    }
}
