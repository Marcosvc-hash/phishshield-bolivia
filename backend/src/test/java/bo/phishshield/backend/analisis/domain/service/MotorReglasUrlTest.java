package bo.phishshield.backend.analisis.domain.service;

import bo.phishshield.backend.analisis.domain.model.DominioListado;
import bo.phishshield.backend.analisis.domain.model.DominioOficial;
import bo.phishshield.backend.analisis.domain.model.Indicador;
import bo.phishshield.backend.analisis.domain.model.UrlAnalizada;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MotorReglasUrlTest {

    private static final List<DominioOficial> OFICIALES = List.of(
            new DominioOficial("Banco Nacional de Bolivia", "bnb.com.bo"),
            new DominioOficial("Banco Union", "bancounion.com.bo"),
            new DominioOficial("Impuestos Nacionales", "impuestos.gob.bo"),
            new DominioOficial("Tigo Bolivia", "tigo.com.bo"),
            new DominioOficial("Banco BISA", "bisa.com")
    );

    private static final Set<String> LISTA_BLANCA = Set.of("visa.com");

    private static final Map<String, BigDecimal> PESOS = Map.of(
            "URL_IP_LITERAL", new BigDecimal("30.00"),
            "URL_SIN_HTTPS", new BigDecimal("8.00"),
            "URL_TYPOSQUATTING", new BigDecimal("45.00"),
            "URL_TLD_SOSPECHOSO", new BigDecimal("12.00"),
            "URL_SUBDOMINIOS", new BigDecimal("10.00"),
            "URL_LISTA_NEGRA", new BigDecimal("70.00"),
            "URL_RUTA_SENSIBLE", new BigDecimal("10.00")
    );

    private MotorReglasUrl motor() {
        return new MotorReglasUrl(List.of(
                new ReglaIpLiteral(),
                new ReglaSinHttps(),
                new ReglaTldSospechoso(),
                new ReglaSubdominios(),
                new ReglaRutaSensible(),
                new ReglaTyposquatting(OFICIALES, LISTA_BLANCA)
        ));
    }

    private List<String> codigosDisparados(String url) {
        return motor().evaluar(UrlAnalizada.de(url), PESOS)
                .stream()
                .map(Indicador::codigoRegla)
                .toList();
    }

    @Test
    void sitio_legitimo_no_dispara_ninguna_regla() {
        assertTrue(codigosDisparados("https://www.bnb.com.bo/").isEmpty());
        assertTrue(codigosDisparados("https://impuestos.gob.bo/tramites").isEmpty());
    }

    @Test
    void url_con_ip_literal_y_sin_https() {
        List<String> codigos = codigosDisparados("http://192.168.1.50/banca/login");

        assertTrue(codigos.contains("URL_IP_LITERAL"));
        assertTrue(codigos.contains("URL_SIN_HTTPS"));
        assertFalse(codigos.contains("URL_SUBDOMINIOS"));
    }

    @Test
    void tld_abusado_dispara_la_regla() {
        assertTrue(codigosDisparados("https://premios-tigo-bo.xyz/sorteo")
                .contains("URL_TLD_SOSPECHOSO"));
    }

    @Test
    void detecta_dominio_casi_identico() {
        List<Indicador> indicadores = motor()
                .evaluar(UrlAnalizada.de("https://bancounlon.com.bo/login"), PESOS);

        assertTrue(indicadores.stream()
                .anyMatch(i -> i.codigoRegla().equals("URL_TYPOSQUATTING")));
    }

    @Test
    void el_peso_viene_del_mapa_no_de_la_regla() {
        Map<String, BigDecimal> pesosBajos = Map.of("URL_SIN_HTTPS", new BigDecimal("2.00"));

        List<Indicador> indicadores = motor()
                .evaluar(UrlAnalizada.de("http://sitio-cualquiera.com"), pesosBajos);

        Indicador sinHttps = indicadores.stream()
                .filter(i -> i.codigoRegla().equals("URL_SIN_HTTPS"))
                .findFirst()
                .orElseThrow();

        assertEquals(new BigDecimal("2.00"), sinHttps.aporte());
    }

    @Test
    void regla_sin_peso_registrado_aporta_cero() {
        List<Indicador> indicadores = motor()
                .evaluar(UrlAnalizada.de("http://sitio-cualquiera.com"), Map.of());

        assertFalse(indicadores.isEmpty());
        assertTrue(indicadores.stream()
                .allMatch(i -> i.aporte().compareTo(BigDecimal.ZERO) == 0));
    }

    @Test
    void caso_real_del_seed_bnb_bo_seguro() {
        List<String> codigos = codigosDisparados("http://bnb-bo-seguro.com/login/verificar-cuenta");

        assertTrue(codigos.contains("URL_SIN_HTTPS"));
        assertTrue(codigos.contains("URL_TYPOSQUATTING"),
                "Deberia detectar que imita al BNB, pero los codigos fueron: " + codigos);
    }
    @Test
    void no_confunde_dominios_que_solo_contienen_las_letras() {
        assertFalse(codigosDisparados("https://urbnb.com/alquileres")
                .contains("URL_TYPOSQUATTING"));
        assertFalse(codigosDisparados("https://elbnbmejor.com")
                .contains("URL_TYPOSQUATTING"));
    }

    @Test
    void detecta_marca_incrustada_entre_palabras() {
        assertTrue(codigosDisparados("http://bnb-bo-seguro.com/login")
                .contains("URL_TYPOSQUATTING"));
        assertTrue(codigosDisparados("https://verificacion-bancounion.net")
                .contains("URL_TYPOSQUATTING"));
        assertTrue(codigosDisparados("https://impuestos-gob-bo.info/nit")
                .contains("URL_TYPOSQUATTING"));
    }

    // ---- Lista blanca ----

    @Test
    void la_lista_blanca_evita_el_falso_positivo_de_visa() {
        assertFalse(codigosDisparados("https://www.visa.com").contains("URL_TYPOSQUATTING"));

        // Sin lista blanca, el mismo dominio si se reporta (distancia 1 con bisa.com)
        List<Indicador> sinListaBlanca = new MotorReglasUrl(List.of(new ReglaTyposquatting(OFICIALES)))
                .evaluar(UrlAnalizada.de("https://www.visa.com"), PESOS);
        assertEquals(1, sinListaBlanca.size());
    }

    /** La tolerancia no cambio: las imitaciones de una letra en marcas cortas se siguen detectando. */
    @Test
    void la_lista_blanca_no_afloja_la_deteccion_de_marcas_cortas() {
        assertTrue(codigosDisparados("http://bnv.com.bo").contains("URL_TYPOSQUATTING"));
        assertTrue(codigosDisparados("https://tiqo.com.bo").contains("URL_TYPOSQUATTING"));
    }

    // ---- Dominio oficial escondido en los subdominios ----

    private Optional<Indicador> indicadorTyposquatting(String url) {
        return motor().evaluar(UrlAnalizada.de(url), PESOS).stream()
                .filter(i -> i.codigoRegla().equals("URL_TYPOSQUATTING"))
                .findFirst();
    }

    @Test
    void detecta_un_dominio_oficial_usado_como_subdominio() {
        Indicador indicador = indicadorTyposquatting(
                "http://bnb.com.bo.verificar.cuenta.segura-login.xyz/").orElseThrow();

        assertEquals("Usa 'bnb.com.bo' como subdominio, pero el dominio real es segura-login.xyz",
                indicador.valor());
    }

    @Test
    void un_subdominio_del_dominio_oficial_no_dispara_la_regla() {
        assertTrue(indicadorTyposquatting("https://banca.bnb.com.bo/").isEmpty());
    }

    @Test
    void el_dominio_oficial_debe_aparecer_como_etiquetas_completas() {
        // "mibnb.com.bo" no es "bnb.com.bo": no debe disparar el caso de subdominio
        assertTrue(indicadorTyposquatting("https://mibnb.com.bo.ejemplo.net/").isEmpty());
    }

    // ---- Lista negra ----

    private static final List<DominioListado> LISTA_NEGRA = List.of(
            new DominioListado("bnb-bo-seguro.com", "Clon de la banca en linea del BNB"),
            new DominioListado("impuestos-gob-bo.info", null)
    );

    private List<Indicador> evaluarConListaNegra(String url) {
        MotorReglasUrl conListaNegra = new MotorReglasUrl(List.of(
                new ReglaSinHttps(),
                new ReglaListaNegra(LISTA_NEGRA)
        ));
        return conListaNegra.evaluar(UrlAnalizada.de(url), PESOS);
    }

    private Optional<Indicador> indicadorListaNegra(String url) {
        return evaluarConListaNegra(url).stream()
                .filter(i -> i.codigoRegla().equals("URL_LISTA_NEGRA"))
                .findFirst();
    }

    @Test
    void lista_negra_detecta_el_dominio_y_sus_subdominios() {
        Indicador exacto = indicadorListaNegra("http://bnb-bo-seguro.com/login").orElseThrow();
        assertEquals("bnb-bo-seguro.com figura en la lista negra de PhishShield: "
                + "Clon de la banca en linea del BNB", exacto.valor());
        assertEquals(new BigDecimal("70.00"), exacto.aporte());

        assertTrue(indicadorListaNegra("https://login.bnb-bo-seguro.com/x").isPresent());

        // Sin motivo registrado, la evidencia no lleva ": ..."
        assertEquals("impuestos-gob-bo.info figura en la lista negra de PhishShield",
                indicadorListaNegra("https://impuestos-gob-bo.info/nit").orElseThrow().valor());
    }

    @Test
    void lista_negra_no_confunde_dominios_que_solo_terminan_igual() {
        assertTrue(indicadorListaNegra("http://otrobnb-bo-seguro.com/login").isEmpty());
        assertTrue(indicadorListaNegra("https://bnb-bo-seguro.com.ejemplo.net").isEmpty());
        assertTrue(indicadorListaNegra("https://www.bnb.com.bo/").isEmpty());
    }
}