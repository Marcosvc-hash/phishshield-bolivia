package bo.phishshield.backend.qa.infrastructure.ejecutor;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Los casos programados deben coincidir con el catalogo de la base
 * (database/initdb/04_qa.sql): un @CasoQA con un codigo que no existe en
 * qa_casos nunca se podria ejecutar.
 */
class RegistroCasosQATest {

    private static final Pattern FILA_CATALOGO = Pattern.compile(
            "\\('(TC-[A-Z0-9]+-\\d{3})',\\s*'(\\w+)',\\s*'(\\w+)',\\s*'(\\w+)'");

    /** codigo -> herramienta (api, ui, carga, modelo), leido del script del catalogo. */
    private static Map<String, String> catalogo() throws IOException {
        String sql = Files.readString(Path.of("..", "database", "initdb", "04_qa.sql"), StandardCharsets.UTF_8);
        Map<String, String> casos = new TreeMap<>();
        Matcher m = FILA_CATALOGO.matcher(sql);
        while (m.find()) {
            casos.put(m.group(1), m.group(4));
        }
        return casos;
    }

    @Test
    void cada_caso_programado_existe_en_el_catalogo() throws IOException {
        Map<String, String> catalogo = catalogo();
        assertEquals(28, catalogo.size(), "El catálogo tiene 28 casos");

        for (String codigo : new RegistroCasosQA().codigos()) {
            assertTrue(catalogo.containsKey(codigo), codigo + " no existe en 04_qa.sql");
        }
    }

    @Test
    void los_casos_de_api_estan_programados() throws IOException {
        RegistroCasosQA registro = new RegistroCasosQA();
        catalogo().forEach((codigo, herramienta) -> {
            if (herramienta.equals("api")) {
                assertTrue(registro.metodo(codigo).isPresent(), "Falta programar " + codigo);
            }
        });
    }

    @Test
    void los_siete_casos_de_interfaz_estan_programados() throws IOException {
        RegistroCasosQA registro = new RegistroCasosQA();
        long ui = catalogo().entrySet().stream().filter(e -> e.getValue().equals("ui")).peek(e ->
                assertTrue(registro.metodo(e.getKey()).isPresent(), "Falta programar " + e.getKey())).count();
        assertEquals(7, ui);
    }

    @Test
    void los_veintiocho_casos_del_catalogo_estan_programados() throws IOException {
        RegistroCasosQA registro = new RegistroCasosQA();
        catalogo().keySet().forEach(codigo ->
                assertTrue(registro.metodo(codigo).isPresent(), "Falta programar " + codigo));
        assertEquals(28, registro.codigos().size());
    }

    @Test
    void el_nombre_de_las_capturas_es_corto_y_sin_tildes() {
        assertEquals("pulsar-analizar-con-el-campo-vacio", NavegadorQA.nombreArchivo("Pulsar \"Analizar\" con el campo vacío"));
        assertTrue(NavegadorQA.nombreArchivo("x".repeat(100)).length() <= 40);
    }

    @Test
    void un_codigo_programado_dos_veces_se_detecta() {
        class Duplicados {
            @org.junit.jupiter.api.Test
            @CasoQA("TC-TST-001")
            void uno() {
            }

            @org.junit.jupiter.api.Test
            @CasoQA("TC-TST-001")
            void otro() {
            }
        }
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new RegistroCasosQA(java.util.List.of(Duplicados.class)));
        assertTrue(ex.getMessage().contains("TC-TST-001"));
    }
}
