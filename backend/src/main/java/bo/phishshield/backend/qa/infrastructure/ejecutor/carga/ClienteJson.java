package bo.phishshield.backend.qa.infrastructure.ejecutor.carga;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Cliente HTTP minimo para recorrer el lote de robustez (80 peticiones):
 * no deja cada peticion en la evidencia, solo devuelve el estado y el JSON.
 */
public final class ClienteJson {

    /** Respuesta de una peticion: estado HTTP (0 si no hubo respuesta) y cuerpo JSON como mapa. */
    public record Respuesta(int estado, Map<String, Object> cuerpo, String error) {

        public boolean ok() {
            return estado >= 200 && estado < 300 && cuerpo != null;
        }
    }

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final TypeReference<Map<String, Object>> MAPA = new TypeReference<>() { };

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public Respuesta post(String url, Map<String, Object> cuerpo) {
        try {
            HttpRequest peticion = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(cuerpo)))
                    .build();
            HttpResponse<String> r = http.send(peticion, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> mapa = r.body() == null || r.body().isBlank() ? null : JSON.readValue(r.body(), MAPA);
            return new Respuesta(r.statusCode(), mapa, null);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new Respuesta(0, null, "interrumpida");
        } catch (Exception ex) {
            return new Respuesta(0, null, ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }
}
