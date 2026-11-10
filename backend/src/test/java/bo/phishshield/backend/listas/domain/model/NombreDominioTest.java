package bo.phishshield.backend.listas.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class NombreDominioTest {

    @Test
    void normaliza_a_minusculas_sin_espacios_ni_punto_final() {
        assertEquals("banco-falso.com", NombreDominio.de("  Banco-Falso.COM.  ").valor());
        assertEquals("xn--bancounin-u9a.com.bo", NombreDominio.de("XN--BANCOUNIN-U9A.com.bo").valor());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://banco-falso.com",      // con esquema
            "banco-falso.com/login",       // con ruta
            "banco-falso.com:8080",        // con puerto
            "usuario@banco-falso.com",     // con usuario
            "banco falso.com",             // con espacio
            "-banco.com",                  // guion al principio
            "banco-.com",                  // guion al final
            "banco..com",                  // etiqueta vacia
            "localhost",                   // sin extension
            "192.168.1.10",                // IP
            "banco.c0m",                   // TLD con digito
            "bancó.com",                   // no ASCII
            "com.bo",                      // sufijo publico entero
            "xyz",                         // solo TLD
            "",
            "   "
    })
    void rechaza_formatos_invalidos(String texto) {
        assertThrows(IllegalArgumentException.class, () -> NombreDominio.de(texto));
    }

    @Test
    void rechaza_null_y_etiquetas_de_mas_de_63_caracteres() {
        assertThrows(IllegalArgumentException.class, () -> NombreDominio.de(null));
        assertThrows(IllegalArgumentException.class, () -> NombreDominio.de("a".repeat(64) + ".com"));
        assertDoesNotThrow(() -> NombreDominio.de("a".repeat(63) + ".com"));
    }

    @Test
    void calcula_el_dominio_registrable_igual_que_el_motor() {
        assertEquals("bnb.com.bo", NombreDominio.de("login.bnb.com.bo").registrable());
        assertTrue(NombreDominio.de("bnb.com.bo").esRegistrable());
        assertFalse(NombreDominio.de("login.bnb.com.bo").esRegistrable());
        assertTrue(NombreDominio.de("premios-tigo-bo.xyz").esRegistrable());
    }

    @Test
    void en_la_lista_blanca_solo_va_el_dominio_registrable() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> DominioEnLista.nuevo(NombreDominio.de("www.bnb.com.bo"), TipoLista.BLANCA, "Oficial", null));
        assertTrue(error.getMessage().contains("bnb.com.bo"));

        // En la negra un subdominio si sirve: la regla compara por etiquetas completas
        assertDoesNotThrow(() ->
                DominioEnLista.nuevo(NombreDominio.de("login.sitio-gratis.com"), TipoLista.NEGRA, "Clon", null));
    }

    @Test
    void la_lista_gris_no_admite_altas_desde_el_panel_y_el_motivo_es_obligatorio() {
        NombreDominio d = NombreDominio.de("premios.xyz");
        assertThrows(IllegalArgumentException.class, () -> DominioEnLista.nuevo(d, TipoLista.GRIS, "x", null));
        assertThrows(IllegalArgumentException.class, () -> DominioEnLista.nuevo(d, TipoLista.NEGRA, "  ", null));
        assertThrows(IllegalArgumentException.class,
                () -> DominioEnLista.nuevo(d, TipoLista.NEGRA, "x".repeat(DominioEnLista.MAXIMO_MOTIVO + 1), null));
    }

    @Test
    void las_listas_opuestas() {
        assertEquals(TipoLista.NEGRA, TipoLista.BLANCA.opuesta());
        assertEquals(TipoLista.BLANCA, TipoLista.NEGRA.opuesta());
        assertNull(TipoLista.GRIS.opuesta());
        assertThrows(IllegalArgumentException.class, () -> TipoLista.desdeValorBd("roja"));
    }
}
