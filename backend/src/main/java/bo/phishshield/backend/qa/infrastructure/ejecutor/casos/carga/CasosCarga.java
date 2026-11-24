package bo.phishshield.backend.qa.infrastructure.ejecutor.casos.carga;

import bo.phishshield.backend.qa.domain.model.EstadisticasLatencia;
import bo.phishshield.backend.qa.domain.model.MetricaRendimiento;
import bo.phishshield.backend.qa.infrastructure.ejecutor.CasoQA;
import bo.phishshield.backend.qa.infrastructure.ejecutor.ClienteApiQA;
import bo.phishshield.backend.qa.infrastructure.ejecutor.ContextoCaso;
import bo.phishshield.backend.qa.infrastructure.ejecutor.ExtensionQA;
import bo.phishshield.backend.qa.infrastructure.ejecutor.FalloInfraestructuraException;
import bo.phishshield.backend.qa.infrastructure.ejecutor.carga.GeneradorCarga;
import bo.phishshield.backend.qa.infrastructure.ejecutor.carga.GeneradorCarga.Corrida;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas no funcionales de carga con el generador propio: Performance
 * (TC-PERF-001/002), Estres (TC-STR-001) y Soak (TC-SOAK-001).
 *
 * Valores por defecto cortos, para la demo. Se cambian con los parametros
 * de la ejecucion: usuarios, peticionesPorUsuario, umbralP95Ms,
 * umbralErrorPct, escalones, umbralErrorEstresPct, minutos, pausaMs.
 *
 * Las URL llevan la marca de la ejecucion. Con https, sin ruta sensible y
 * una extension comun, el aporte del modelo de IA no llega a 40 puntos:
 * los analisis salen seguros y la carga no llena la base de alertas.
 */
@ExtendWith(ExtensionQA.class)
public class CasosCarga {

    @Test
    @CasoQA("TC-PERF-001")
    void cargaConUrlNuevas(ContextoCaso ctx) {
        ctx.requiereBackend();
        Parametros p = Parametros.de(ctx);
        String marca = ctx.marca();
        calentar(ctx);

        Corrida corrida = ctx.paso(p.usuarios + " usuarios concurrentes x " + p.porUsuario
                + " peticiones con URL nuevas (reglas + IA)", () -> {
            Corrida c = generador(ctx).porCantidad(p.usuarios, p.porUsuario,
                    i -> "https://www.carga-" + marca + "-" + i + ".com/");
            ctx.observar(resumen(c.estadisticas()));
            return c;
        });
        evaluar(ctx, corrida.estadisticas(), p);
    }

    @Test
    @CasoQA("TC-PERF-002")
    void cargaConUrlRepetidasDeLaCache(ContextoCaso ctx) {
        ctx.requiereBackend();
        Parametros p = Parametros.de(ctx);
        String[] grupo = new String[5];
        for (int i = 0; i < grupo.length; i++) {
            grupo[i] = "https://www.cache-" + ctx.marca() + "-" + i + ".com/";
        }
        calentar(ctx);

        ctx.paso("Preparar: analizar una vez las " + grupo.length + " URL del grupo para que queden en la caché", () -> {
            GeneradorCarga g = generador(ctx);
            long inicio = System.nanoTime();
            int errores = 0;
            for (String url : grupo) {
                errores += g.enviar(url, inicio).error() ? 1 : 0;
            }
            ctx.observar(grupo.length + " URL analizadas; errores: " + errores);
            assertEquals(0, errores, "No se pudo preparar la caché");
        });

        Corrida corrida = ctx.paso(p.usuarios + " usuarios concurrentes x " + p.porUsuario
                + " peticiones repitiendo las URL de la caché", () -> {
            Corrida c = generador(ctx).porCantidad(p.usuarios, p.porUsuario, i -> grupo[i % grupo.length]);
            ctx.observar(resumen(c.estadisticas()));
            return c;
        });
        evaluar(ctx, corrida.estadisticas(), p);
    }

    @Test
    @CasoQA("TC-STR-001")
    void estresPorEscalones(ContextoCaso ctx) {
        ctx.requiereBackend();
        Parametros p = Parametros.de(ctx);
        int[] escalones = escalones(ctx.parametro("escalones", "10,20,40,80"));
        int porUsuario = ctx.parametroEntero("peticionesPorUsuario", 5);
        double umbralError = ctx.parametroDecimal("umbralErrorEstresPct", 5.0);
        String marca = ctx.marca();
        calentar(ctx);

        int degradado = 0;
        for (int n = 0; n < escalones.length; n++) {
            int numero = n + 1;
            int usuarios = escalones[n];
            EstadisticasLatencia e = ctx.paso("Escalón " + numero + ": " + usuarios + " usuarios x " + porUsuario
                    + " peticiones", () -> {
                EstadisticasLatencia est = generador(ctx).porCantidad(usuarios, porUsuario,
                        i -> "https://www.estres-" + marca + "-e" + numero + "-" + i + ".com/").estadisticas();
                ctx.observar(resumen(est));
                return est;
            });
            ctx.metrica(MetricaRendimiento.informativa("Usuarios concurrentes", BigDecimal.valueOf(usuarios), "usuarios")
                    .enEscalon(numero));
            ctx.metrica(MetricaRendimiento.conMaximo("p95", BigDecimal.valueOf(e.p95Ms()), "ms",
                    BigDecimal.valueOf(p.umbralP95Ms)).enEscalon(numero));
            ctx.metrica(MetricaRendimiento.conMaximo("Tasa de error", e.tasaErrorPct(), "%",
                    BigDecimal.valueOf(umbralError)).enEscalon(numero));
            ctx.metrica(MetricaRendimiento.informativa("Throughput", e.throughput(), "pet/s").enEscalon(numero));

            boolean seDegrada = e.p95Ms() >= p.umbralP95Ms || e.tasaErrorPct().doubleValue() > umbralError;
            if (seDegrada) {
                degradado = numero;
                ctx.paso("El servicio se degradó en el escalón " + numero + " (" + usuarios
                        + " usuarios): no se sube más la carga", () -> ctx.observar(resumen(e)));
                break;
            }
        }
        int primerDegradado = degradado;
        ctx.metrica(MetricaRendimiento.informativa("Primer escalón degradado (0 = ninguno)",
                BigDecimal.valueOf(primerDegradado), "escalón"));

        ctx.paso("Recuperación: tras 2 s, una petición normal debe responder 200 por debajo del umbral", () -> {
            dormir(2000);
            GeneradorCarga.Muestra m = generador(ctx).enviar("https://www.recuperacion-" + marca + ".com/", System.nanoTime());
            ctx.observar("HTTP " + m.estado() + " en " + m.latenciaMs() + " ms");
            ctx.metrica(MetricaRendimiento.conMaximo("Latencia de recuperación", BigDecimal.valueOf(m.latenciaMs()), "ms",
                    BigDecimal.valueOf(p.umbralP95Ms)));
            assertEquals(200, m.estado(), "La petición de recuperación no respondió 200");
            assertTrue(m.latenciaMs() < p.umbralP95Ms, "La petición de recuperación tardó " + m.latenciaMs()
                    + " ms (umbral " + p.umbralP95Ms + " ms)");
        });
    }

    @Test
    @CasoQA("TC-SOAK-001")
    void resistenciaConCargaConstante(ContextoCaso ctx) {
        ctx.requiereBackend();
        int usuarios = ctx.parametroEntero("usuarios", 5);
        double minutos = ctx.parametroDecimal("minutos", 2.0);
        int pausaMs = ctx.parametroEntero("pausaMs", 200);
        double umbralError = ctx.parametroDecimal("umbralErrorPct", 1.0);
        if (minutos <= 0 || minutos > 30) {
            Assumptions.abort("El parámetro minutos debe estar entre 0 y 30 (se pidió " + minutos + ")");
        }
        long duracionMs = Math.round(minutos * 60_000);
        String marca = ctx.marca();
        calentar(ctx);

        Corrida corrida = ctx.paso(usuarios + " usuarios constantes durante " + formatear(minutos)
                + " min (pausa de " + pausaMs + " ms entre peticiones)", () -> {
            Corrida c = generador(ctx).porTiempo(usuarios, Duration.ofMillis(duracionMs), Duration.ofMillis(pausaMs),
                    i -> "https://www.soak-" + marca + "-" + i + ".com/", ctx::detenida);
            ctx.observar(resumen(c.estadisticas()));
            return c;
        });
        if (corrida.detenida()) {
            Assumptions.abort("Se detuvo la ejecución durante la carga: no se puede evaluar la resistencia");
        }

        EstadisticasLatencia total = corrida.estadisticas();
        long cuarto = corrida.duracionMs() / 4;
        EstadisticasLatencia primero = corrida.tramo(0, cuarto);
        EstadisticasLatencia ultimo = corrida.tramo(corrida.duracionMs() - cuarto, corrida.duracionMs() + 1);
        // Piso de 20 ms: con latencias muy chicas, 3 ms -> 6 ms seria "+100 %" sin significar nada
        long base = Math.max(20, primero.p95Ms());
        BigDecimal crecimiento = BigDecimal.valueOf((ultimo.p95Ms() - base) * 100L)
                .divide(BigDecimal.valueOf(base), 2, RoundingMode.HALF_UP).max(BigDecimal.ZERO);

        ctx.metrica(MetricaRendimiento.informativa("Peticiones totales", BigDecimal.valueOf(total.peticiones()), "peticiones"));
        ctx.metrica(MetricaRendimiento.informativa("Throughput", total.throughput(), "pet/s"));
        ctx.metrica(MetricaRendimiento.conMaximo("Tasa de error", total.tasaErrorPct(), "%", BigDecimal.valueOf(umbralError)));
        ctx.metrica(MetricaRendimiento.informativa("p95 del primer tramo", BigDecimal.valueOf(primero.p95Ms()), "ms"));
        ctx.metrica(MetricaRendimiento.informativa("p95 del último tramo", BigDecimal.valueOf(ultimo.p95Ms()), "ms"));
        ctx.metrica(MetricaRendimiento.conMaximo("Crecimiento del p95", crecimiento, "%", BigDecimal.valueOf(50)));

        ctx.paso("Verificar tasa de error < " + formatear(umbralError) + " % y que el p95 del último tramo no supere "
                + "en más de 50 % al del primero", () -> {
            ctx.observar("Error " + total.tasaErrorPct() + " %; p95 primer tramo " + primero.p95Ms() + " ms, último tramo "
                    + ultimo.p95Ms() + " ms (crecimiento " + crecimiento + " %)");
            assertTrue(total.tasaErrorPct().doubleValue() < umbralError, "Tasa de error " + total.tasaErrorPct() + " %");
            assertTrue(crecimiento.compareTo(BigDecimal.valueOf(50)) < 0,
                    "El p95 creció " + crecimiento + " % entre el primer y el último tramo");
        });
    }

    // ------------------------------------------------------------------
    // Ayudas
    // ------------------------------------------------------------------

    /** Valores comunes de las pruebas de carga, leidos de los parametros de la ejecucion. */
    private record Parametros(int usuarios, int porUsuario, long umbralP95Ms, double umbralErrorPct) {
        static Parametros de(ContextoCaso ctx) {
            int usuarios = ctx.parametroEntero("usuarios", 20);
            int porUsuario = ctx.parametroEntero("peticionesPorUsuario", 5);
            if (usuarios < 1 || usuarios > 500 || porUsuario < 1 || porUsuario > 1000) {
                Assumptions.abort("usuarios debe estar entre 1 y 500 y peticionesPorUsuario entre 1 y 1000");
            }
            return new Parametros(usuarios, porUsuario, ctx.parametroEntero("umbralP95Ms", 2000),
                    ctx.parametroDecimal("umbralErrorPct", 1.0));
        }
    }

    private static GeneradorCarga generador(ContextoCaso ctx) {
        return new GeneradorCarga(ctx.entorno().baseUrl());
    }

    /**
     * Una peticion normal antes de cargar: si el backend no responde, la
     * prueba falla como infraestructura en vez de medir 100 % de errores.
     */
    private static void calentar(ContextoCaso ctx) {
        ctx.paso("Calentamiento: una petición normal a POST /api/analisis", () -> {
            Response r;
            try {
                r = ctx.api().sinSesion()
                        .body(ClienteApiQA.json("url", "https://www.calentamiento-" + ctx.marca() + ".com/", "origen", "api"))
                        .post("/api/analisis");
            } catch (RuntimeException ex) {
                throw new FalloInfraestructuraException("El backend no responde en " + ctx.entorno().baseUrl(), ex);
            }
            ctx.observar("HTTP " + r.statusCode() + " en " + r.time() + " ms");
            if (r.statusCode() != 200) {
                throw new FalloInfraestructuraException("La petición de calentamiento respondió HTTP " + r.statusCode());
            }
        });
    }

    /** Registra las metricas de una corrida de performance y verifica p95 y tasa de error. */
    private static void evaluar(ContextoCaso ctx, EstadisticasLatencia e, Parametros p) {
        ctx.metrica(MetricaRendimiento.informativa("Peticiones totales", BigDecimal.valueOf(e.peticiones()), "peticiones"));
        ctx.metrica(MetricaRendimiento.informativa("p50", BigDecimal.valueOf(e.p50Ms()), "ms"));
        ctx.metrica(MetricaRendimiento.conMaximo("p95", BigDecimal.valueOf(e.p95Ms()), "ms", BigDecimal.valueOf(p.umbralP95Ms)));
        ctx.metrica(MetricaRendimiento.informativa("p99", BigDecimal.valueOf(e.p99Ms()), "ms"));
        ctx.metrica(MetricaRendimiento.informativa("Latencia media", BigDecimal.valueOf(e.mediaMs()), "ms"));
        ctx.metrica(MetricaRendimiento.informativa("Latencia máxima", BigDecimal.valueOf(e.maximaMs()), "ms"));
        ctx.metrica(MetricaRendimiento.informativa("Throughput", e.throughput(), "pet/s"));
        ctx.metrica(MetricaRendimiento.conMaximo("Tasa de error", e.tasaErrorPct(), "%", BigDecimal.valueOf(p.umbralErrorPct)));

        ctx.paso("Verificar p95 < " + p.umbralP95Ms + " ms y tasa de error < " + formatear(p.umbralErrorPct) + " %", () -> {
            ctx.observar("p95 = " + e.p95Ms() + " ms; tasa de error = " + e.tasaErrorPct() + " %");
            assertTrue(e.p95Ms() < p.umbralP95Ms, "p95 de " + e.p95Ms() + " ms (umbral " + p.umbralP95Ms + " ms)");
            assertTrue(e.tasaErrorPct().doubleValue() < p.umbralErrorPct,
                    "Tasa de error de " + e.tasaErrorPct() + " % (umbral " + formatear(p.umbralErrorPct) + " %)");
        });
    }

    static String resumen(EstadisticasLatencia e) {
        return e.peticiones() + " peticiones en " + e.duracionMs() + " ms; p50 " + e.p50Ms() + " ms, p95 " + e.p95Ms()
                + " ms, p99 " + e.p99Ms() + " ms, máx " + e.maximaMs() + " ms; " + e.throughput() + " pet/s; errores "
                + e.errores() + " (" + e.tasaErrorPct() + " %)";
    }

    static int[] escalones(String texto) {
        List<Integer> valores = new ArrayList<>();
        for (String parte : texto.split("[,; ]+")) {
            if (!parte.isBlank()) {
                try {
                    int n = Integer.parseInt(parte.strip());
                    if (n < 1 || n > 500) {
                        throw new NumberFormatException();
                    }
                    valores.add(n);
                } catch (NumberFormatException ex) {
                    Assumptions.abort("Escalones inválidos: " + texto + " (enteros entre 1 y 500 separados por coma)");
                }
            }
        }
        if (valores.isEmpty()) {
            Assumptions.abort("No se indicó ningún escalón");
        }
        return valores.stream().mapToInt(Integer::intValue).toArray();
    }

    private static String formatear(double valor) {
        return valor == Math.rint(valor) ? String.valueOf((long) valor) : String.valueOf(valor).replace('.', ',');
    }

    private static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
