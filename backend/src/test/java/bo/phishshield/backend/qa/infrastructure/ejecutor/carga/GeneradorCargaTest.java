package bo.phishshield.backend.qa.infrastructure.ejecutor.carga;

import bo.phishshield.backend.qa.domain.model.EstadisticasLatencia;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El generador de carga contra un servidor HTTP local de mentira (el que
 * trae el JDK): cuenta peticiones, errores y respeta el tiempo y la orden
 * de detener.
 */
class GeneradorCargaTest {

    private HttpServer servidor;
    private final AtomicInteger recibidas = new AtomicInteger();
    private final Set<String> cuerpos = ConcurrentHashMap.newKeySet();

    @BeforeEach
    void levantar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        servidor.createContext("/api/analisis", intercambio -> {
            String cuerpo = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            cuerpos.add(cuerpo);
            int n = recibidas.incrementAndGet();
            // Las URL marcadas con "-error-" responden 500, para contar errores
            int estado = cuerpo.contains("-error-") ? 500 : 200;
            byte[] respuesta = ("{\"n\":" + n + "}").getBytes(StandardCharsets.UTF_8);
            intercambio.sendResponseHeaders(estado, respuesta.length);
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(respuesta);
            }
        });
        servidor.start();
    }

    @AfterEach
    void bajar() {
        servidor.stop(0);
    }

    private String base() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    @Test
    void por_cantidad_manda_usuarios_por_peticiones_con_url_distintas() {
        GeneradorCarga.Corrida c = new GeneradorCarga(base()).porCantidad(8, 5,
                i -> "https://www.u" + i + (i % 10 == 0 ? "-error-" : "") + ".com/");

        EstadisticasLatencia e = c.estadisticas();
        assertEquals(40, e.peticiones());
        assertEquals(40, recibidas.get());
        assertEquals(40, cuerpos.size(), "Cada petición lleva una URL distinta");
        assertEquals(4, e.errores(), "Las URL 0, 10, 20 y 30 responden 500");
        assertEquals("10.00", e.tasaErrorPct().toPlainString());
        assertTrue(cuerpos.stream().allMatch(b -> b.contains("\"origen\":\"api\"")));
    }

    @Test
    void por_tiempo_corre_hasta_que_se_acaba_el_tiempo() {
        long inicio = System.nanoTime();
        GeneradorCarga.Corrida c = new GeneradorCarga(base()).porTiempo(3, Duration.ofMillis(600),
                Duration.ofMillis(50), i -> "https://www.t" + i + ".com/", () -> false);
        long ms = (System.nanoTime() - inicio) / 1_000_000;

        assertFalse(c.detenida());
        assertTrue(ms >= 550 && ms < 5000, "Duró " + ms + " ms");
        assertTrue(c.estadisticas().peticiones() >= 6, "Hubo " + c.estadisticas().peticiones() + " peticiones");
        assertEquals(0, c.estadisticas().errores());
        // Las muestras se pueden separar por tramos de tiempo
        assertTrue(c.tramo(0, c.duracionMs() / 2).peticiones() > 0);
    }

    @Test
    void se_corta_cuando_se_pide_detener() {
        AtomicInteger consultas = new AtomicInteger();
        GeneradorCarga.Corrida c = new GeneradorCarga(base()).porTiempo(2, Duration.ofSeconds(30),
                Duration.ofMillis(20), i -> "https://www.d" + i + ".com/", () -> consultas.incrementAndGet() > 6);

        assertTrue(c.detenida());
        assertTrue(c.duracionMs() < 5000, "No esperó los 30 s");
    }

    @Test
    void sin_servidor_cada_peticion_cuenta_como_error() {
        GeneradorCarga.Muestra m = new GeneradorCarga("http://127.0.0.1:1").enviar("https://x.com/", System.nanoTime());

        assertTrue(m.error());
        assertEquals(0, m.estado());
    }
}
