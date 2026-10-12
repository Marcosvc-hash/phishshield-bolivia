package bo.phishshield.backend.analisis.infrastructure.ia;

import bo.phishshield.backend.analisis.domain.model.PrediccionIa;
import bo.phishshield.backend.analisis.domain.port.out.ModeloIaPort;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Adaptador de salida hacia el servicio de IA (ia/servicio.py, FastAPI).
 *
 * Cualquier falla (servicio apagado, timeout, respuesta rara) se registra con
 * log.warn y devuelve vacio: el analisis sigue solo con las reglas.
 */
@Component
public class ModeloIaRestAdapter implements ModeloIaPort {

    private static final Logger log = LoggerFactory.getLogger(ModeloIaRestAdapter.class);
    private static final String HACIA_PHISHING = "phishing";

    private final RestClient cliente;
    private final boolean habilitado;

    public ModeloIaRestAdapter(@Value("${phishshield.ia.url}") String url,
                               @Value("${phishshield.ia.timeout}") Duration timeout,
                               @Value("${phishshield.ia.habilitada:true}") boolean habilitado) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(timeout);
        fabrica.setReadTimeout(timeout);
        this.cliente = RestClient.builder().baseUrl(url).requestFactory(fabrica).build();
        this.habilitado = habilitado;
        log.info("Modelo de IA en {} (timeout {} ms, {})", url, timeout.toMillis(),
                habilitado ? "habilitado" : "deshabilitado");
    }

    @Override
    public Optional<PrediccionIa> predecir(String url) {
        if (!habilitado) {
            return Optional.empty();
        }
        try {
            RespuestaIa r = cliente.post()
                    .uri("/predecir")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("url", url))
                    .retrieve()
                    .body(RespuestaIa.class);
            if (r == null || r.probabilidadPhishing() == null || r.umbral() == null) {
                log.warn("El servicio de IA devolvio una respuesta incompleta; se sigue solo con las reglas");
                return Optional.empty();
            }
            List<String> senales = r.senales() == null ? List.of() : r.senales().stream()
                    .filter(s -> HACIA_PHISHING.equals(s.direccion()))
                    .map(SenalIa::texto)
                    .toList();
            return Optional.of(new PrediccionIa(r.probabilidadPhishing(), r.umbral(), senales,
                    r.modelo(), r.version()));
        } catch (RuntimeException e) {
            log.warn("Servicio de IA no disponible ({}); se sigue solo con las reglas", e.getMessage());
            return Optional.empty();
        }
    }

    record RespuestaIa(@JsonProperty("probabilidad_phishing") BigDecimal probabilidadPhishing,
                       @JsonProperty("es_phishing") Boolean esPhishing,
                       BigDecimal umbral,
                       List<SenalIa> senales,
                       String modelo,
                       String version) {
    }

    record SenalIa(String texto, BigDecimal aporte, String direccion) {
    }
}
