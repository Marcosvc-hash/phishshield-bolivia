package bo.phishshield.backend.analisis.infrastructure.ia;

import bo.phishshield.backend.analisis.domain.model.PrediccionIa;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Fallas reales de red, sin Spring: el adaptador nunca debe romper el analisis. */
class ModeloIaRestAdapterTest {

    @Test
    void servicio_apagado_devuelve_vacio() throws Exception {
        int puertoLibre;
        try (ServerSocket s = new ServerSocket(0)) {
            puertoLibre = s.getLocalPort();
        }
        ModeloIaRestAdapter adaptador = new ModeloIaRestAdapter(
                "http://localhost:" + puertoLibre, Duration.ofSeconds(2), true);

        assertTrue(adaptador.predecir("https://ejemplo.com").isEmpty());
    }

    /** Un servidor que acepta la conexion pero nunca responde: corta por timeout. */
    @Test
    void servicio_colgado_corta_por_timeout() throws Exception {
        try (ServerSocket mudo = new ServerSocket(0)) {
            ModeloIaRestAdapter adaptador = new ModeloIaRestAdapter(
                    "http://localhost:" + mudo.getLocalPort(), Duration.ofMillis(300), true);

            long inicio = System.nanoTime();
            assertTrue(adaptador.predecir("https://ejemplo.com").isEmpty());
            long ms = (System.nanoTime() - inicio) / 1_000_000;
            assertTrue(ms < 2_000, "Tardo " + ms + " ms");
        }
    }

    /** Misma forma que la respuesta real de ia/servicio.py. */
    @Test
    void lee_la_respuesta_del_servicio_y_se_queda_con_las_senales_hacia_phishing() throws Exception {
        String json = """
                {"probabilidad_phishing": 0.9575, "es_phishing": true, "umbral": 0.5527,
                 "senales": [
                   {"texto": "el dominio tiene 1 guion", "aporte": 1.2, "direccion": "phishing"},
                   {"texto": "no tiene subdominios", "aporte": -0.4, "direccion": "legitimo"},
                   {"texto": "el dominio contiene 'verificacion'", "aporte": 0.9, "direccion": "phishing"}],
                 "modelo": "phishshield-url-ia", "version": "1.0.0"}""";
        HttpServer servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/predecir", intercambio -> {
            byte[] cuerpo = json.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(200, cuerpo.length);
            intercambio.getResponseBody().write(cuerpo);
            intercambio.close();
        });
        servidor.start();
        try {
            ModeloIaRestAdapter adaptador = new ModeloIaRestAdapter(
                    "http://localhost:" + servidor.getAddress().getPort(), Duration.ofSeconds(2), true);

            PrediccionIa p = adaptador.predecir("https://bancounion-verificacion.net/").orElseThrow();

            assertEquals(new BigDecimal("0.9575"), p.probabilidad());
            assertEquals(new BigDecimal("0.5527"), p.umbral());
            assertEquals(List.of("el dominio tiene 1 guion", "el dominio contiene 'verificacion'"), p.senales());
            assertEquals("1.0.0", p.version());
        } finally {
            servidor.stop(0);
        }
    }

    @Test
    void deshabilitado_no_consulta() {
        ModeloIaRestAdapter adaptador = new ModeloIaRestAdapter(
                "http://no-existe.invalid", Duration.ofSeconds(2), false);

        assertTrue(adaptador.predecir("https://ejemplo.com").isEmpty());
    }
}
