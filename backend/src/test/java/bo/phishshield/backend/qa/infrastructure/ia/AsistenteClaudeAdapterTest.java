package bo.phishshield.backend.qa.infrastructure.ia;

import bo.phishshield.backend.qa.domain.model.AnalisisFallo;
import bo.phishshield.backend.qa.domain.model.ContextoFallo;
import bo.phishshield.backend.qa.domain.model.EstadoPaso;
import bo.phishshield.backend.qa.domain.model.PasoEjecutado;
import bo.phishshield.backend.qa.domain.model.TipoFallo;
import bo.phishshield.backend.qa.infrastructure.config.AsistentePropiedades;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El adaptador del asistente contra un servidor local que imita la Messages
 * API: revisa lo que se manda (encabezados y cuerpo) y como se lee la respuesta.
 */
class AsistenteClaudeAdapterTest {

    private HttpServer servidor;
    private final AtomicReference<String> cuerpoRecibido = new AtomicReference<>();
    private final AtomicReference<Map<String, List<String>>> encabezados = new AtomicReference<>();
    private volatile int estado = 200;
    private volatile String respuesta;

    @BeforeEach
    void levantar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/v1/messages", intercambio -> {
            cuerpoRecibido.set(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            encabezados.set(Map.copyOf(intercambio.getRequestHeaders()));
            byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(estado, bytes.length);
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(bytes);
            }
        });
        servidor.start();
    }

    @AfterEach
    void bajar() {
        servidor.stop(0);
    }

    private AsistenteClaudeAdapter adaptador(String clave) {
        return new AsistenteClaudeAdapter(new AsistentePropiedades(clave, null,
                "http://127.0.0.1:" + servidor.getAddress().getPort()));
    }

    private static ContextoFallo contexto() {
        OffsetDateTime ahora = OffsetDateTime.now();
        return new ContextoFallo("TC-SAN-003", "Typosquatting de BancoSol", TipoFallo.FUNCIONAL,
                "expected: <phishing> but was: <seguro>", "Verificar el resultado", List.of(
                new PasoEjecutado(1, "Enviar POST /api/analisis", "HTTP 200, resultado = seguro", EstadoPaso.OK,
                        null, "POST http://localhost:8080/api/analisis\n-> HTTP 200", ahora)));
    }

    private static String mensaje(String stopReason, String textoJson) {
        return """
                {"id":"msg_1","type":"message","role":"assistant","model":"claude-opus-5-5",
                 "content":[{"type":"thinking","thinking":"","signature":"x"},{"type":"text","text":%s}],
                 "stop_reason":"%s","usage":{"input_tokens":10,"output_tokens":20}}
                """.formatted(textoJson == null ? "\"\"" : "\"" + textoJson.replace("\"", "\\\"") + "\"", stopReason);
    }

    @Test
    void sin_clave_no_esta_disponible_y_no_llama_a_la_api() {
        AsistenteClaudeAdapter asistente = adaptador("");

        assertFalse(asistente.estaDisponible());
        assertEquals(Optional.empty(), asistente.analizarFallo(contexto()));
        assertNull(cuerpoRecibido.get());
    }

    @Test
    void manda_la_clave_la_version_y_un_esquema_json_y_lee_la_respuesta() {
        respuesta = mensaje("end_turn",
                "{\"causaProbable\":\"BancoSol no está entre las entidades\",\"evidencia\":\"Paso 1: resultado seguro\"}");

        AnalisisFallo analisis = adaptador("clave-de-prueba").analizarFallo(contexto()).orElseThrow();

        assertEquals("BancoSol no está entre las entidades", analisis.causaProbable());
        assertEquals("Paso 1: resultado seguro", analisis.evidencia());
        Map<String, List<String>> h = encabezados.get();
        assertEquals(List.of("clave-de-prueba"), h.get("X-api-key"));
        assertEquals(List.of(AsistenteClaudeAdapter.VERSION_API), h.get("Anthropic-version"));
        assertEquals(List.of(AsistenteClaudeAdapter.BETA_RESPALDO), h.get("Anthropic-beta"));
        String cuerpo = cuerpoRecibido.get();
        assertTrue(cuerpo.contains("\"model\":\"claude-opus-5-5\""));
        assertTrue(cuerpo.contains("\"json_schema\""));
        assertTrue(cuerpo.contains("\"fallbacks\":\"default\""));
        assertTrue(cuerpo.contains("TC-SAN-003"));
        assertFalse(cuerpo.contains("clave-de-prueba"), "La clave va solo en el encabezado");
    }

    @Test
    void un_rechazo_o_un_error_devuelven_vacio_sin_lanzar() {
        AsistenteClaudeAdapter asistente = adaptador("clave");

        respuesta = mensaje("refusal", null);
        assertEquals(Optional.empty(), asistente.analizarFallo(contexto()));

        respuesta = mensaje("end_turn", "esto no es json");
        assertEquals(Optional.empty(), asistente.analizarFallo(contexto()));

        estado = 401;
        respuesta = "{\"type\":\"error\",\"error\":{\"type\":\"authentication_error\",\"message\":\"invalid x-api-key\"}}";
        assertEquals(Optional.empty(), asistente.analizarFallo(contexto()));
    }
}
