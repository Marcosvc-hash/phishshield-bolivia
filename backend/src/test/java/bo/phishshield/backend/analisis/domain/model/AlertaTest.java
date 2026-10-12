package bo.phishshield.backend.analisis.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AlertaTest {

    private static Analisis analisisCon(String puntaje, OrigenAnalisis origen) {
        return Analisis.deUrl(UrlAnalizada.de("http://ejemplo.com"), origen,
                List.of(new Indicador("URL_PRUEBA", "prueba", new BigDecimal(puntaje))));
    }

    @Test
    void la_severidad_depende_del_puntaje() {
        assertEquals(Severidad.CRITICA, Severidad.desdePuntaje(new BigDecimal("90")));
        assertEquals(Severidad.ALTA, Severidad.desdePuntaje(new BigDecimal("89.99")));
        assertEquals(Severidad.ALTA, Severidad.desdePuntaje(new BigDecimal("70")));
        assertEquals(Severidad.MEDIA, Severidad.desdePuntaje(new BigDecimal("69.99")));
    }

    @Test
    void el_canal_depende_del_origen() {
        assertEquals(CanalAlerta.EMAIL, CanalAlerta.desdeOrigen(OrigenAnalisis.PLUGIN_CORREO));
        assertEquals(CanalAlerta.EXTENSION, CanalAlerta.desdeOrigen(OrigenAnalisis.WEB));
        assertEquals(CanalAlerta.EXTENSION, CanalAlerta.desdeOrigen(OrigenAnalisis.EXTENSION));
        assertEquals(CanalAlerta.EXTENSION, CanalAlerta.desdeOrigen(OrigenAnalisis.API));
    }

    @Test
    void un_analisis_seguro_no_tiene_alerta() {
        assertTrue(Alerta.para(analisisCon("39.99", OrigenAnalisis.WEB)).isEmpty());
    }

    @Test
    void un_analisis_sospechoso_tiene_alerta_media() {
        Optional<Alerta> alerta = Alerta.para(analisisCon("40", OrigenAnalisis.PLUGIN_CORREO));

        assertEquals(new Alerta(Severidad.MEDIA, CanalAlerta.EMAIL), alerta.orElseThrow());
    }

    @Test
    void los_valores_coinciden_con_los_check_de_la_base() {
        assertEquals("critica", Severidad.CRITICA.aValorBd());
        assertEquals("plugin_correo", OrigenAnalisis.PLUGIN_CORREO.aValorBd());
        assertEquals("extension", CanalAlerta.EXTENSION.aValorBd());
    }
}
