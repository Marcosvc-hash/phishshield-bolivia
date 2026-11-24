package bo.phishshield.backend.qa.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Matriz de confusion y estadisticas de latencia: el calculo de las metricas no funcionales. */
class MetricasNoFuncionalesTest {

    @Test
    void la_matriz_cuenta_cada_url_en_su_casilla() {
        MatrizConfusion m = MatrizConfusion.vacia()
                .con(true, true)    // VP
                .con(true, true)    // VP
                .con(true, false)   // FN
                .con(false, true)   // FP
                .con(false, false)  // VN
                .con(false, false); // VN

        assertEquals(new MatrizConfusion(2, 1, 2, 1), m);
        assertEquals(6, m.total());
        assertEquals(new BigDecimal("0.6667"), m.precision(), "2 / (2 + 1)");
        assertEquals(new BigDecimal("0.6667"), m.recall(), "2 / (2 + 1)");
        assertEquals(new BigDecimal("0.6667"), m.f1());
        assertEquals(new BigDecimal("0.6667"), m.exactitud(), "(2 + 2) / 6");
    }

    @Test
    void sin_detecciones_la_precision_es_cero_y_no_divide_por_cero() {
        MatrizConfusion m = MatrizConfusion.vacia().con(true, false).con(false, false);

        assertEquals(new BigDecimal("0.0000"), m.precision());
        assertEquals(new BigDecimal("0.0000"), m.recall());
        assertEquals(new BigDecimal("0.0000"), m.f1());
        assertEquals(new BigDecimal("0.5000"), m.exactitud());
    }

    @Test
    void los_percentiles_usan_el_rango_mas_cercano() {
        List<Long> latencias = new ArrayList<>();
        for (long i = 1; i <= 100; i++) {
            latencias.add(i);
        }

        EstadisticasLatencia e = EstadisticasLatencia.de(latencias, 2, 2000);

        assertEquals(50, e.p50Ms());
        assertEquals(95, e.p95Ms());
        assertEquals(99, e.p99Ms());
        assertEquals(100, e.maximaMs());
        assertEquals(51, e.mediaMs(), "50,5 redondeado");
        assertEquals(new BigDecimal("50.00"), e.throughput(), "100 peticiones en 2 s");
        assertEquals(new BigDecimal("2.00"), e.tasaErrorPct());
    }

    @Test
    void con_pocas_muestras_el_p95_es_la_mayor() {
        EstadisticasLatencia e = EstadisticasLatencia.de(List.of(30L, 10L, 20L), 0, 100);

        assertEquals(20, e.p50Ms());
        assertEquals(30, e.p95Ms());
        assertEquals(30, e.p99Ms());
    }

    @Test
    void sin_muestras_todo_queda_en_cero() {
        EstadisticasLatencia e = EstadisticasLatencia.de(List.of(), 0, 0);

        assertEquals(0, e.peticiones());
        assertEquals(new BigDecimal("0.00"), e.throughput());
        assertEquals(new BigDecimal("0.00"), e.tasaErrorPct());
    }

    @Test
    void la_url_del_lote_necesita_texto_y_tiene_categoria_por_defecto() {
        assertThrows(IllegalArgumentException.class, () -> new UrlEtiquetada(" ", true, "x"));
        assertEquals("sin_categoria", new UrlEtiquetada("https://a.bo", false, null).categoria());
    }
}
