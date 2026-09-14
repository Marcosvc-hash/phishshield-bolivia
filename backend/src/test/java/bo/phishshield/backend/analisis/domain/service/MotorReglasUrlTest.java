package bo.phishshield.backend.analisis.domain.service;

import bo.phishshield.backend.analisis.domain.model.DominioOficial;
import bo.phishshield.backend.analisis.domain.model.Indicador;
import bo.phishshield.backend.analisis.domain.model.UrlAnalizada;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MotorReglasUrlTest {

    private static final List<DominioOficial> OFICIALES = List.of(
            new DominioOficial("Banco Nacional de Bolivia", "bnb.com.bo"),
            new DominioOficial("Banco Union", "bancounion.com.bo"),
            new DominioOficial("Impuestos Nacionales", "impuestos.gob.bo"),
            new DominioOficial("Tigo Bolivia", "tigo.com.bo")
    );

    private static final Map<String, BigDecimal> PESOS = Map.of(
            "URL_IP_LITERAL", new BigDecimal("18.00"),
            "URL_SIN_HTTPS", new BigDecimal("8.00"),
            "URL_TYPOSQUATTING", new BigDecimal("25.00"),
            "URL_TLD_SOSPECHOSO", new BigDecimal("12.00"),
            "URL_SUBDOMINIOS", new BigDecimal("10.00")
    );

    private MotorReglasUrl motor() {
        return new MotorReglasUrl(List.of(
                new ReglaIpLiteral(),
                new ReglaSinHttps(),
                new ReglaTldSospechoso(),
                new ReglaSubdominios(),
                new ReglaTyposquatting(OFICIALES)
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
}