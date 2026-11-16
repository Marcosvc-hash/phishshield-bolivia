package bo.phishshield.backend.analisis.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UrlAnalizadaTest {

    /**
     * "dominio.com." (con un punto al final) es una forma valida de escribir
     * el mismo dominio. El host se guarda sin ese punto para que las reglas
     * comparen siempre contra la misma forma.
     */
    @Test
    void quita_el_punto_final_del_host() {
        UrlAnalizada url = UrlAnalizada.de("http://BNB-BO-Seguro.com./login");

        assertEquals("bnb-bo-seguro.com", url.getHost());
        assertEquals(".com", url.getTld());
        assertEquals("bnb-bo-seguro.com", url.getDominioRegistrable());
        assertEquals("/login", url.getRuta());
    }

    @Test
    void con_punto_final_el_host_es_el_mismo_que_sin_el() {
        assertEquals(UrlAnalizada.de("https://www.bnb.com.bo/").getHost(),
                UrlAnalizada.de("https://www.bnb.com.bo./").getHost());
    }

    /** Lo que escribio la persona se conserva tal cual: es lo que se guarda y se muestra. */
    @Test
    void conserva_la_url_original_como_se_escribio() {
        UrlAnalizada url = UrlAnalizada.de("  http://bnb-bo-seguro.com./login  ");

        assertEquals("http://bnb-bo-seguro.com./login", url.getOriginal());
    }
}
