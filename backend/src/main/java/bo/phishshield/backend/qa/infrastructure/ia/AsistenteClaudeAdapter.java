package bo.phishshield.backend.qa.infrastructure.ia;

import bo.phishshield.backend.qa.domain.model.AnalisisFallo;
import bo.phishshield.backend.qa.domain.model.ContextoFallo;
import bo.phishshield.backend.qa.domain.model.Ejecucion;
import bo.phishshield.backend.qa.domain.model.MetricaRendimiento;
import bo.phishshield.backend.qa.domain.model.PasoEjecutado;
import bo.phishshield.backend.qa.domain.model.ResultadoCaso;
import bo.phishshield.backend.qa.domain.port.out.AsistenteIAPort;
import bo.phishshield.backend.qa.infrastructure.config.AsistentePropiedades;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Asistente de IA del Centro de Pruebas con la Messages API de Anthropic
 * (Claude), por HTTP con RestClient: POST {url}/v1/messages.
 *
 * - Solo esta disponible si existe ANTHROPIC_API_KEY.
 * - Pide la respuesta en JSON con un esquema fijo (output_config.format).
 * - Activa el respaldo del servidor ante un rechazo (fallbacks: "default").
 * - Si la API no responde, rechaza o devuelve algo inesperado: log.warn y
 *   Optional.empty(), igual que el cliente del servicio de IA del analizador.
 * - Nunca decide PASS ni FAIL: solo explica lo que ya decidio la prueba.
 */
public class AsistenteClaudeAdapter implements AsistenteIAPort {

    static final String VERSION_API = "2026-10-21";
    static final String BETA_RESPALDO = "server-side-fallback-2026-07-01";

    private static final Logger log = LoggerFactory.getLogger(AsistenteClaudeAdapter.class);
    private static final int MAXIMO_CONTEXTO = 12_000;
    private static final int MAXIMO_DETALLE_PASO = 800;
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final TypeReference<Map<String, Object>> MAPA = new TypeReference<>() { };

    private static final String SISTEMA = """
            Eres un analista de calidad (QA) que ayuda a leer los resultados del Centro de Pruebas de
            PhishShield Bolivia, un detector de phishing (backend Spring Boot, frontend Angular, modelo de IA
            en Python, base PostgreSQL). Respondes en español sencillo, sin jerga innecesaria.
            El estado de cada caso (PASS, FAIL, SKIPPED, NOT_RUN) ya lo decidió la prueba automatizada: no lo
            cambies ni lo discutas. Los casos se clasifican solo por tipo de prueba (funcional o no funcional)
            y por suite; no uses las palabras "positivo" ni "negativo" para describir casos.
            Basa tus conclusiones solo en la evidencia que recibes y dilo cuando no alcance.""";

    private final AsistentePropiedades propiedades;
    private final RestClient http;

    public AsistenteClaudeAdapter(AsistentePropiedades propiedades) {
        this.propiedades = propiedades;
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(10));
        fabrica.setReadTimeout(Duration.ofSeconds(120));
        this.http = RestClient.builder()
                .baseUrl(propiedades.urlEfectiva())
                .requestFactory(fabrica)
                .build();
    }

    @Override
    public boolean estaDisponible() {
        return propiedades.configurado();
    }

    public String modelo() {
        return propiedades.modeloEfectivo();
    }

    @Override
    public Optional<AnalisisFallo> analizarFallo(ContextoFallo contexto) {
        StringBuilder texto = new StringBuilder()
                .append("Un caso del Centro de Pruebas terminó en FAIL. Propón la causa más probable y cita la ")
                .append("evidencia concreta que la respalda (paso, código HTTP, mensaje). Indica también si parece un ")
                .append("defecto de PhishShield o un problema del entorno.\n\n")
                .append("Caso: ").append(contexto.codigo()).append(" - ").append(contexto.nombreCaso()).append('\n')
                .append("Tipo de fallo informado: ").append(contexto.tipoFallo() == null ? "-" : contexto.tipoFallo().etiqueta()).append('\n')
                .append("Paso donde falló: ").append(nulo(contexto.pasoFallido())).append('\n')
                .append("Mensaje de error: ").append(nulo(contexto.mensajeError())).append("\n\nPasos registrados:\n");
        for (PasoEjecutado p : contexto.pasos()) {
            texto.append(p.numero()).append(". [").append(p.estado().name()).append("] ").append(p.descripcion())
                    .append(" -> ").append(nulo(p.resultadoObservado())).append('\n');
            if (p.detalleApi() != null) {
                texto.append("   Evidencia de API: ").append(recortar(p.detalleApi(), MAXIMO_DETALLE_PASO)).append('\n');
            }
        }
        Map<String, Object> esquema = esquema(Map.of(
                "causaProbable", Map.of("type", "string", "description", "Causa más probable, en una o dos oraciones"),
                "evidencia", Map.of("type", "string", "description", "Evidencia concreta que la respalda")));

        return pedir(recortar(texto.toString(), MAXIMO_CONTEXTO), esquema)
                .map(json -> new AnalisisFallo(String.valueOf(json.get("causaProbable")), String.valueOf(json.get("evidencia"))));
    }

    @Override
    public Optional<String> resumirEjecucion(Ejecucion e) {
        var r = e.resumen();
        StringBuilder texto = new StringBuilder()
                .append("Escribe un resumen ejecutivo breve (máximo 6 oraciones) de esta ejecución para un docente ")
                .append("de Testing: qué se probó, qué pasó, qué falló y qué conviene revisar primero.\n\n")
                .append("Suite: ").append(e.getSuite().etiqueta()).append(" (").append(e.tipo().etiqueta()).append(")\n")
                .append("Estado: ").append(e.getEstado().name()).append(", resultado global: ")
                .append(e.getResultadoGlobal() == null ? "sin resultado" : e.getResultadoGlobal().etiqueta()).append('\n')
                .append("PASS ").append(r.pass()).append(", FAIL ").append(r.fail()).append(", SKIPPED ").append(r.skipped())
                .append(", NOT_RUN ").append(r.notRun()).append("\n\nCasos:\n");
        for (ResultadoCaso c : e.getResultados()) {
            texto.append("- ").append(c.getCodigo()).append(" ").append(c.getCaso().nombre()).append(": ")
                    .append(c.getEstado().name()).append(" - ").append(c.observacionResumida()).append('\n');
            for (MetricaRendimiento m : c.getMetricas()) {
                texto.append("    ").append(m.nombre()).append(m.escalon() == null ? "" : " (escalón " + m.escalon() + ")")
                        .append(" = ").append(m.valor().stripTrailingZeros().toPlainString()).append(' ').append(m.unidad())
                        .append(m.cumple() == null ? "" : (m.cumple() ? " (cumple)" : " (no cumple)")).append('\n');
            }
        }
        Map<String, Object> esquema = esquema(Map.of(
                "resumen", Map.of("type", "string", "description", "Resumen ejecutivo en español")));
        return pedir(recortar(texto.toString(), MAXIMO_CONTEXTO), esquema).map(json -> String.valueOf(json.get("resumen")));
    }

    /** Una llamada a la Messages API que devuelve el JSON pedido, o vacio si algo falla. */
    @SuppressWarnings("unchecked")
    Optional<Map<String, Object>> pedir(String mensaje, Map<String, Object> esquema) {
        if (!estaDisponible()) {
            return Optional.empty();
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("model", propiedades.modeloEfectivo());
        cuerpo.put("max_tokens", 16000);
        cuerpo.put("system", SISTEMA);
        cuerpo.put("messages", List.of(Map.of("role", "user", "content", mensaje)));
        cuerpo.put("output_config", Map.of("effort", "medium",
                "format", Map.of("type", "json_schema", "schema", esquema)));
        cuerpo.put("fallbacks", "default");
        try {
            Map<String, Object> respuesta = http.post()
                    .uri("/v1/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-api-key", propiedades.clave())
                    .header("anthropic-version", VERSION_API)
                    .header("anthropic-beta", BETA_RESPALDO)
                    .body(cuerpo)
                    .retrieve()
                    .body(Map.class);
            if (respuesta == null) {
                log.warn("El asistente de IA devolvió una respuesta vacía");
                return Optional.empty();
            }
            Object motivo = respuesta.get("stop_reason");
            if ("refusal".equals(motivo)) {
                log.warn("El asistente de IA rechazó la solicitud (stop_reason refusal): {}", respuesta.get("stop_details"));
                return Optional.empty();
            }
            if ("max_tokens".equals(motivo)) {
                log.warn("La respuesta del asistente de IA se cortó por max_tokens");
                return Optional.empty();
            }
            String texto = ((List<Map<String, Object>>) respuesta.getOrDefault("content", List.of())).stream()
                    .filter(bloque -> "text".equals(bloque.get("type")))
                    .map(bloque -> String.valueOf(bloque.get("text")))
                    .reduce("", String::concat);
            if (texto.isBlank()) {
                log.warn("El asistente de IA no devolvió texto (stop_reason {})", motivo);
                return Optional.empty();
            }
            return Optional.of(JSON.readValue(texto, MAPA));
        } catch (RuntimeException ex) {
            // Incluye errores HTTP (401 clave invalida, 429, 5xx), timeouts y JSON mal formado
            log.warn("Asistente de IA no disponible ({}): {}", ex.getClass().getSimpleName(), ex.getMessage());
            return Optional.empty();
        }
    }

    private static Map<String, Object> esquema(Map<String, Object> propiedades) {
        return Map.of("type", "object", "properties", propiedades,
                "required", List.copyOf(propiedades.keySet()), "additionalProperties", false);
    }

    private static String nulo(String texto) {
        return texto == null || texto.isBlank() ? "-" : texto;
    }

    private static String recortar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo) + "… (recortado)";
    }
}
