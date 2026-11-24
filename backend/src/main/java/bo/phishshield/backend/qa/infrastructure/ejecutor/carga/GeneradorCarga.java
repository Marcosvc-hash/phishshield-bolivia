package bo.phishshield.backend.qa.infrastructure.ejecutor.carga;

import bo.phishshield.backend.qa.domain.model.EstadisticasLatencia;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.IntFunction;

/**
 * Generador de carga propio: N "usuarios" concurrentes, cada uno en su hilo
 * virtual, mandando POST /api/analisis. Mide la latencia de cada peticion
 * (desde que sale hasta que llega la respuesta completa) y si fallo.
 *
 * Los hilos virtuales (Java 21+) son livianos: crear 80 o 1000 cuesta casi
 * nada, a diferencia de los hilos del sistema. Equivalen, a grandes rasgos,
 * a lanzar muchas Task con async/await en C#.
 */
public final class GeneradorCarga {

    /** Una peticion medida. inicioMs es relativo al arranque de la prueba (para separar tramos). */
    public record Muestra(long inicioMs, long latenciaMs, int estado, boolean error) {
    }

    /** Lo que devuelve cada corrida: las muestras y cuanto duro. */
    public record Corrida(List<Muestra> muestras, long duracionMs, boolean detenida) {

        public EstadisticasLatencia estadisticas() {
            return de(muestras, duracionMs);
        }

        /** Estadisticas solo de las muestras que empezaron entre desdeMs y hastaMs. */
        public EstadisticasLatencia tramo(long desdeMs, long hastaMs) {
            List<Muestra> delTramo = muestras.stream()
                    .filter(m -> m.inicioMs() >= desdeMs && m.inicioMs() < hastaMs)
                    .toList();
            return de(delTramo, Math.max(1, hastaMs - desdeMs));
        }

        private static EstadisticasLatencia de(List<Muestra> muestras, long duracionMs) {
            return EstadisticasLatencia.de(muestras.stream().map(Muestra::latenciaMs).toList(),
                    (int) muestras.stream().filter(Muestra::error).count(), duracionMs);
        }
    }

    private static final Duration TIEMPO_MAXIMO = Duration.ofSeconds(30);

    private final HttpClient http;
    private final String baseUrl;

    public GeneradorCarga(String baseUrl) {
        this.baseUrl = baseUrl;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build();
    }

    /**
     * Cada usuario manda una cantidad fija de peticiones, una tras otra.
     *
     * @param url que URL analizar en la peticion i (i es unico en toda la corrida)
     */
    public Corrida porCantidad(int usuarios, int peticionesPorUsuario, IntFunction<String> url) {
        return correr(usuarios, url, (usuario, contador) -> contador.get() < peticionesPorUsuario,
                Duration.ZERO, () -> false);
    }

    /**
     * Carga constante durante un tiempo: cada usuario manda una peticion,
     * espera la pausa y repite hasta que se acaba el tiempo o se detiene.
     */
    public Corrida porTiempo(int usuarios, Duration duracion, Duration pausa, IntFunction<String> url,
                             BooleanSupplier detener) {
        long fin = System.nanoTime() + duracion.toNanos();
        return correr(usuarios, url, (usuario, contador) -> System.nanoTime() < fin, pausa, detener);
    }

    @FunctionalInterface
    private interface Seguir {
        boolean test(int usuario, AtomicInteger hechasPorEsteUsuario);
    }

    private Corrida correr(int usuarios, IntFunction<String> url, Seguir seguir, Duration pausa,
                           BooleanSupplier detener) {
        ConcurrentLinkedQueue<Muestra> muestras = new ConcurrentLinkedQueue<>();
        AtomicInteger indice = new AtomicInteger();
        CountDownLatch largada = new CountDownLatch(1);
        long inicio = System.nanoTime();
        AtomicBoolean detenida = new AtomicBoolean();

        try (ExecutorService hilos = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int u = 0; u < usuarios; u++) {
                int usuario = u;
                hilos.submit(() -> {
                    AtomicInteger hechas = new AtomicInteger();
                    try {
                        largada.await();
                        while (seguir.test(usuario, hechas)) {
                            if (detener.getAsBoolean()) {
                                detenida.set(true);
                                return;
                            }
                            muestras.add(enviar(url.apply(indice.getAndIncrement()), inicio));
                            hechas.incrementAndGet();
                            if (!pausa.isZero()) {
                                Thread.sleep(pausa);
                            }
                        }
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            largada.countDown();
        } // close() espera a que terminen todos los usuarios

        long duracionMs = (System.nanoTime() - inicio) / 1_000_000;
        return new Corrida(new ArrayList<>(muestras), duracionMs, detenida.get());
    }

    /** Una peticion: POST /api/analisis con la URL; error si no hay respuesta o no es 2xx. */
    public Muestra enviar(String url, long inicioPruebaNs) {
        String cuerpo = "{\"url\":\"" + url.replace("\\", "\\\\").replace("\"", "\\\"") + "\",\"origen\":\"api\"}";
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(baseUrl + "/api/analisis"))
                .timeout(TIEMPO_MAXIMO)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                .build();
        long antes = System.nanoTime();
        int estado = 0;
        boolean error;
        try {
            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
            estado = respuesta.statusCode();
            error = estado < 200 || estado >= 300;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            error = true;
        } catch (Exception ex) {
            error = true;
        }
        long despues = System.nanoTime();
        return new Muestra((antes - inicioPruebaNs) / 1_000_000, (despues - antes) / 1_000_000, estado, error);
    }
}
