package bo.phishshield.backend.analisis.domain.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Una URL ya parseada y normalizada. Se construye una sola vez por analisis
 * y las reglas heuristicas la interrogan sin volver a parsear la cadena.
 */
public final class UrlAnalizada {

    private static final List<String> ESQUEMAS_VALIDOS = List.of("http", "https");
    /**
     * Sufijos publicos de dos etiquetas. Sin esta lista, bnb.com.bo parece
     * tener un subdominio cuando en realidad no tiene ninguno.
     */
    private static final Set<String> SUFIJOS_COMPUESTOS = Set.of(
            // Bolivia
            "com.bo", "gob.bo", "edu.bo", "org.bo", "net.bo", "mil.bo", "tv.bo", "web.bo",
            // Region
            "com.ar", "com.br", "com.mx", "com.pe", "com.co", "com.uy", "com.py", "com.ve",
            "com.ec", "com.cl",
            // Otros frecuentes
            "co.uk", "org.uk", "com.es"
    );

    private final String original;
    private final String esquema;
    private final String host;
    private final String ruta;

    private UrlAnalizada(String original, String esquema, String host, String ruta) {
        this.original = original;
        this.esquema = esquema;
        this.host = host;
        this.ruta = ruta;
    }

    public static UrlAnalizada de(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new UrlInvalidaException("La URL no puede estar vacia");
        }

        String limpia = texto.trim();

        try {
            URI uri = new URI(limpia);

            if (uri.getScheme() == null || uri.getHost() == null) {
                throw new UrlInvalidaException("URL sin esquema o sin host: " + limpia);
            }

            String esquema = uri.getScheme().toLowerCase();
            if (!ESQUEMAS_VALIDOS.contains(esquema)) {
                throw new UrlInvalidaException("Esquema no soportado: " + esquema);
            }

            String host = uri.getHost().toLowerCase();
            if (host.length() > 255) {
                throw new UrlInvalidaException("Dominio demasiado largo");
            }

            String ruta = uri.getPath() == null ? "" : uri.getPath();

            return new UrlAnalizada(limpia, esquema, host, ruta);

        } catch (URISyntaxException e) {
            throw new UrlInvalidaException("URL mal formada: " + limpia);
        }
    }

    // ---- Preguntas que haran las reglas heuristicas ----

    public boolean usaHttps() {
        return "https".equals(esquema);
    }

    public boolean hostEsIpLiteral() {
        return host.matches("^\\d{1,3}(\\.\\d{1,3}){3}$");
    }

    /** Devuelve el TLD con punto incluido: ".xyz", ".bo", ".com" */
    public String getTld() {
        int ultimoPunto = host.lastIndexOf('.');
        return ultimoPunto < 0 ? "" : host.substring(ultimoPunto);
    }

    /**
     * El sufijo publico del host.
     * "mail.banca.bnb.com.bo" devuelve "com.bo"
     * "premios-tigo-bo.xyz"   devuelve "xyz"
     */
    public String getSufijoPublico() {
        String[] partes = host.split("\\.");
        if (partes.length >= 2) {
            String ultimasDos = partes[partes.length - 2] + "." + partes[partes.length - 1];
            if (SUFIJOS_COMPUESTOS.contains(ultimasDos)) {
                return ultimasDos;
            }
        }
        return partes[partes.length - 1];
    }

    /**
     * El dominio que alguien efectivamente registro.
     * "mail.banca.bnb.com.bo" devuelve "bnb.com.bo"
     * Es el que hay que comparar contra entidades_suplantadas.
     */
    public String getDominioRegistrable() {
        if (hostEsIpLiteral()) {
            return host;
        }
        String sufijo = getSufijoPublico();
        if (host.length() <= sufijo.length()) {
            return host;
        }
        String resto = host.substring(0, host.length() - sufijo.length() - 1);
        String[] partes = resto.split("\\.");
        return partes[partes.length - 1] + "." + sufijo;
    }

    /**
     * Etiquetas que sobran por delante del dominio registrable.
     * "mail.banca.bnb.com.bo" devuelve 2
     * "bnb.com.bo"            devuelve 0
     */
    public int getCantidadSubdominios() {
        if (hostEsIpLiteral()) {
            return 0;
        }
        int etiquetasHost = host.split("\\.").length;
        int etiquetasRegistrable = getDominioRegistrable().split("\\.").length;
        return Math.max(0, etiquetasHost - etiquetasRegistrable);
    }

    /** Partes del host separadas por punto. Util para comparar con entidades conocidas. */
    public List<String> getEtiquetas() {
        return Arrays.asList(host.split("\\."));
    }

    public String getOriginal() { return original; }
    public String getEsquema() { return esquema; }
    public String getHost() { return host; }
    public String getRuta() { return ruta; }
}