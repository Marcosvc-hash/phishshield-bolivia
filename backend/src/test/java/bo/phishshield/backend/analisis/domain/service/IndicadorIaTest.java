package bo.phishshield.backend.analisis.domain.service;

import bo.phishshield.backend.analisis.domain.model.Indicador;
import bo.phishshield.backend.analisis.domain.model.PrediccionIa;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IndicadorIaTest {

    private static final BigDecimal PESO = new BigDecimal("40.00");
    private static final BigDecimal UMBRAL = new BigDecimal("0.5527");

    private static PrediccionIa prediccion(String probabilidad, String... senales) {
        return new PrediccionIa(new BigDecimal(probabilidad), UMBRAL, List.of(senales),
                "phishshield-url-ia", "1.0.0");
    }

    @Test
    void el_aporte_es_probabilidad_por_peso() {
        Indicador i = IndicadorIa.desde(prediccion("0.8734"), PESO).orElseThrow();

        assertEquals("IA_MODELO_URL", i.codigoRegla());
        assertEquals(new BigDecimal("34.94"), i.aporte());   // 0.8734 x 40 = 34.936
    }

    @Test
    void justo_en_el_umbral_si_aporta() {
        assertTrue(IndicadorIa.desde(prediccion("0.5527"), PESO).isPresent());
        assertTrue(IndicadorIa.desde(prediccion("0.5526"), PESO).isEmpty());
    }

    @Test
    void la_evidencia_tiene_porcentaje_y_hasta_3_senales() {
        Indicador i = IndicadorIa.desde(prediccion("0.97", "a", "b", "c", "d"), PESO).orElseThrow();

        assertEquals("Modelo de IA 1.0.0: 97% de probabilidad de phishing. Senales: a; b; c", i.valor());
    }

    @Test
    void si_no_entra_en_255_caracteres_quita_senales() {
        String larga = "x".repeat(120);
        Indicador i = IndicadorIa.desde(prediccion("0.97", larga, larga, larga), PESO).orElseThrow();

        assertTrue(i.valor().length() <= 255, i.valor());
        assertTrue(i.valor().endsWith(larga));
        assertEquals(1, i.valor().split(";").length);
    }

    @Test
    void la_probabilidad_fuera_de_rango_se_rechaza() {
        assertThrows(IllegalArgumentException.class, () -> prediccion("1.2"));
        assertThrows(IllegalArgumentException.class, () -> prediccion("-0.1"));
    }
}
