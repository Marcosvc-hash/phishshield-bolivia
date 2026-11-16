package bo.phishshield.backend.analisis.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AnalisisTest {

    @Test
    void normaliza_el_host_a_minusculas() {
        UrlAnalizada url = UrlAnalizada.de("HTTP://BNB-BO-SEGURO.COM/Login");

        assertEquals("bnb-bo-seguro.com", url.getHost());
        assertEquals("http", url.getEsquema());
        assertFalse(url.usaHttps());
    }

    @Test
    void extrae_tld_y_subdominios() {
        assertEquals(".xyz", UrlAnalizada.de("https://premios-tigo-bo.xyz/sorteo").getTld());
        assertEquals(0, UrlAnalizada.de("https://premios-tigo-bo.xyz/sorteo").getCantidadSubdominios());
        assertEquals(2, UrlAnalizada.de("https://mail.banca.bnb.com.bo").getCantidadSubdominios());
    }

    @Test
    void detecta_ip_literal() {
        assertTrue(UrlAnalizada.de("http://192.168.1.50/login").hostEsIpLiteral());
        assertFalse(UrlAnalizada.de("https://www.bnb.com.bo").hostEsIpLiteral());
    }

    @Test
    void rechaza_url_mal_formada() {
        assertThrows(UrlInvalidaException.class, () -> UrlAnalizada.de("no-es-una-url"));
        assertThrows(UrlInvalidaException.class, () -> UrlAnalizada.de(""));
        assertThrows(UrlInvalidaException.class, () -> UrlAnalizada.de("ftp://archivos.com"));
    }

    @Test
    void puntaje_alto_produce_veredicto_phishing() {
        UrlAnalizada url = UrlAnalizada.de("http://bnb-bo-seguro.com/login/verificar-cuenta");

        List<Indicador> indicadores = List.of(
                new Indicador("URL_TYPOSQUATTING", "bnb-bo-seguro.com ~ bnb.com.bo", new BigDecimal("25.00")),
                new Indicador("URL_SIN_HTTPS", "esquema http", new BigDecimal("8.00")),
                new Indicador("URL_DOMINIO_NUEVO", "registrado hace 6 dias", new BigDecimal("15.00")),
                new Indicador("TXT_PIDE_CREDENCIAL", "solicita contrasena", new BigDecimal("30.00"))
        );

        Analisis analisis = Analisis.deUrl(url, OrigenAnalisis.WEB, indicadores);

        assertEquals(new BigDecimal("78.00"), analisis.getNivelRiesgo());
        assertEquals(Resultado.PHISHING, analisis.getResultado());
        assertTrue(analisis.requiereAlerta());
        assertEquals("bnb-bo-seguro.com", analisis.getDominio());
    }

    @Test
    void sin_indicadores_el_veredicto_es_seguro() {
        Analisis analisis = Analisis.deUrl(
                UrlAnalizada.de("https://www.bnb.com.bo/"),
                OrigenAnalisis.WEB,
                List.of());

        assertEquals(new BigDecimal("0.00"), analisis.getNivelRiesgo());
        assertEquals(Resultado.SEGURO, analisis.getResultado());
        assertFalse(analisis.requiereAlerta());
    }

    @Test
    void el_puntaje_nunca_pasa_de_cien() {
        List<Indicador> exagerados = List.of(
                new Indicador("R1", null, new BigDecimal("60.00")),
                new Indicador("R2", null, new BigDecimal("60.00")),
                new Indicador("R3", null, new BigDecimal("60.00"))
        );

        Analisis analisis = Analisis.deUrl(
                UrlAnalizada.de("http://sitio-malo.xyz"), OrigenAnalisis.API, exagerados);

        assertEquals(new BigDecimal("100.00"), analisis.getNivelRiesgo());
    }

    @Test
    void el_hash_tiene_64_caracteres() {
        Analisis analisis = Analisis.deUrl(
                UrlAnalizada.de("https://www.bnb.com.bo/"),
                OrigenAnalisis.WEB,
                List.of());

        assertEquals(64, analisis.getContenidoHash().length());
        assertTrue(analisis.getContenidoHash().matches("[0-9a-f]{64}"));
    }
    @Test
    void extrae_el_tld() {
        assertEquals(".xyz", UrlAnalizada.de("https://premios-tigo-bo.xyz/sorteo").getTld());
        assertEquals(".bo", UrlAnalizada.de("https://www.bnb.com.bo").getTld());
    }

    @Test
    void cuenta_subdominios_respetando_sufijos_bolivianos() {
        assertEquals(0, UrlAnalizada.de("https://premios-tigo-bo.xyz/sorteo").getCantidadSubdominios());
        assertEquals(0, UrlAnalizada.de("https://bnb.com.bo").getCantidadSubdominios());
        assertEquals(1, UrlAnalizada.de("https://www.bancounion.com.bo").getCantidadSubdominios());
        assertEquals(2, UrlAnalizada.de("https://mail.banca.bnb.com.bo").getCantidadSubdominios());
        assertEquals(0, UrlAnalizada.de("http://192.168.1.50/login").getCantidadSubdominios());
    }

    @Test
    void identifica_el_dominio_registrable() {
        assertEquals("bnb.com.bo", UrlAnalizada.de("https://mail.banca.bnb.com.bo").getDominioRegistrable());
        assertEquals("impuestos.gob.bo", UrlAnalizada.de("https://www.impuestos.gob.bo/tramites").getDominioRegistrable());
        assertEquals("bnb-bo-seguro.com", UrlAnalizada.de("http://bnb-bo-seguro.com/login").getDominioRegistrable());
        assertEquals("premios-tigo-bo.xyz", UrlAnalizada.de("https://premios-tigo-bo.xyz/sorteo").getDominioRegistrable());
    }

    // ---- Reutilizar un analisis de la cache (hallazgo 12) ----

    private static final OffsetDateTime CALCULO = OffsetDateTime.of(2026, 11, 16, 9, 0, 0, 0, ZoneOffset.ofHours(-4));

    /** Un analisis ya guardado, calculado por el usuario 1 de la empresa 1. */
    private static Analisis guardadoPorUsuario1() {
        Analisis original = Analisis.deUrl(
                UrlAnalizada.de("http://bnb-bo-seguro.com/login"), OrigenAnalisis.WEB,
                List.of(new Indicador("URL_LISTA_NEGRA", "bnb-bo-seguro.com", new BigDecimal("70.00")),
                        new Indicador("URL_SIN_HTTPS", "esquema http", new BigDecimal("8.00"))));
        original.asignarContexto(1L, 1L, 4L);
        original.asignarIdentidad(10L, UUID.randomUUID(), CALCULO);
        return original;
    }

    @Test
    void reutilizar_copia_indicadores_puntaje_y_veredicto_pero_no_la_identidad() {
        Analisis original = guardadoPorUsuario1();

        Analisis copia = Analisis.reutilizando(original, OrigenAnalisis.EXTENSION);

        assertEquals(original.getIndicadores(), copia.getIndicadores());
        assertEquals(original.getNivelRiesgo(), copia.getNivelRiesgo());
        assertEquals(Resultado.PHISHING, copia.getResultado());
        assertEquals(original.getContenidoHash(), copia.getContenidoHash());
        assertEquals(OrigenAnalisis.EXTENSION, copia.getOrigen(), "El origen es el del pedido nuevo");
        // Es una fila nueva: sin id, sin usuario ni empresa hasta que el caso de uso los asigne
        assertNull(copia.getId());
        assertNull(copia.getIdUsuario());
        assertNull(copia.getIdEmpresa());
        assertNull(copia.getFechaAnalisis());
    }

    @Test
    void la_copia_recuerda_cuando_se_calculo_el_original() {
        Analisis copia = Analisis.reutilizando(guardadoPorUsuario1(), OrigenAnalisis.WEB);

        assertEquals(CALCULO, copia.getCalculadoEn());
        assertEquals(CALCULO, copia.momentoDelCalculo());
    }

    /** Si la copia de una copia tomara la fecha de la primera copia, la vigencia se renovaria sin recalcular. */
    @Test
    void la_copia_de_una_copia_conserva_la_fecha_del_calculo_original() {
        Analisis primeraCopia = Analisis.reutilizando(guardadoPorUsuario1(), OrigenAnalisis.WEB);
        primeraCopia.asignarIdentidad(11L, UUID.randomUUID(), CALCULO.plusHours(20));

        Analisis segundaCopia = Analisis.reutilizando(primeraCopia, OrigenAnalisis.WEB);

        assertEquals(CALCULO, segundaCopia.momentoDelCalculo());
    }

    @Test
    void un_analisis_calculado_no_tiene_calculado_en_y_su_calculo_es_su_fecha() {
        Analisis original = guardadoPorUsuario1();

        assertNull(original.getCalculadoEn());
        assertEquals(CALCULO, original.momentoDelCalculo());
    }

    @Test
    void no_se_reutiliza_un_analisis_sin_guardar() {
        Analisis sinGuardar = Analisis.deUrl(UrlAnalizada.de("https://ejemplo.com"), OrigenAnalisis.WEB, List.of());

        assertThrows(IllegalArgumentException.class, () -> Analisis.reutilizando(sinGuardar, OrigenAnalisis.WEB));
        assertThrows(IllegalArgumentException.class, () -> Analisis.reutilizando(null, OrigenAnalisis.WEB));
        assertThrows(IllegalArgumentException.class, () -> Analisis.reutilizando(guardadoPorUsuario1(), null));
    }
}