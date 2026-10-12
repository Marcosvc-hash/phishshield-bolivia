package bo.phishshield.backend.analisis.domain.service;

import bo.phishshield.backend.analisis.domain.model.UrlAnalizada;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ReglaRutaSensibleTest {

    private final ReglaRutaSensible regla = new ReglaRutaSensible();

    private Optional<String> evaluar(String url) {
        return regla.evaluar(UrlAnalizada.de(url));
    }

    @Test
    void detecta_palabras_por_segmento_y_lista_las_encontradas() {
        assertEquals(Optional.of("La ruta contiene palabras sensibles: banca, login"),
                evaluar("http://192.168.1.50/banca/login"));
    }

    @Test
    void detecta_palabras_dentro_de_un_segmento_compuesto() {
        assertEquals(Optional.of("La ruta contiene palabras sensibles: verificar, cuenta"),
                evaluar("https://ejemplo.com/verificar-cuenta.php"));
        assertTrue(evaluar("https://ejemplo.com/clientes/actualizar-datos").isPresent());
    }

    @Test
    void no_distingue_mayusculas_ni_tildes() {
        assertTrue(evaluar("https://ejemplo.com/Login.PHP").isPresent());
        assertTrue(evaluar("https://ejemplo.com/cambiar%20Contrase%C3%B1a").isPresent());
    }

    @Test
    void compara_palabras_completas_y_no_subcadenas() {
        assertTrue(evaluar("https://ejemplo.com/blogin-info").isEmpty());
        assertTrue(evaluar("https://ejemplo.com/spinner").isEmpty());
        assertTrue(evaluar("https://ejemplo.com/cuentas-claras").isEmpty());
    }

    @Test
    void sin_ruta_o_con_ruta_inocua_no_se_dispara() {
        assertTrue(evaluar("http://192.168.1.1").isEmpty());
        assertTrue(evaluar("https://premios-tigo-bo.xyz/sorteo").isEmpty());
    }

    @Test
    void solo_mira_la_ruta_y_no_el_host_ni_los_parametros() {
        assertTrue(evaluar("https://login.ejemplo.com/inicio").isEmpty());
        assertTrue(evaluar("https://ejemplo.com/inicio?next=login").isEmpty());
    }
}
