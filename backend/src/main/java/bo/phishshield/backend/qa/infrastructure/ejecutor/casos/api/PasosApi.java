package bo.phishshield.backend.qa.infrastructure.ejecutor.casos.api;

import bo.phishshield.backend.qa.infrastructure.ejecutor.ClienteApiQA;
import bo.phishshield.backend.qa.infrastructure.ejecutor.ContextoCaso;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Pasos que repiten varios casos de API (iniciar sesion, analizar una URL),
 * para que cada caso se lea como su descripcion en el catalogo.
 */
final class PasosApi {

    private PasosApi() {
    }

    /** Paso "Iniciar sesión con el administrador de prueba": reutiliza el token si sigue vigente. */
    static void iniciarSesion(ContextoCaso ctx) {
        ctx.paso("Iniciar sesión con el administrador de prueba (QA_TEST_USER)", () -> {
            ctx.api().tokenAdmin();
            ctx.observar("Token de acceso vigente hasta las " + ctx.api().horaVencimientoToken()
                    + " (hora de Bolivia)");
        });
    }

    /** POST /api/analisis con origen "api"; con o sin sesion. Verifica el 200 dentro del paso. */
    static Response analizar(ContextoCaso ctx, String url, boolean conSesion) {
        return ctx.paso("Enviar POST /api/analisis con la URL " + url
                + (conSesion ? " (con sesión)" : " (sin sesión)"), () -> {
            Response r = (conSesion ? ctx.api().conSesion() : ctx.api().sinSesion())
                    .body(ClienteApiQA.json("url", url, "origen", "api"))
                    .post("/api/analisis");
            ctx.observar(r.statusCode() == 200 ? "HTTP 200, " + resumenAnalisis(r.jsonPath()) : "HTTP " + r.statusCode());
            assertEquals(200, r.statusCode(), "Código HTTP del análisis");
            return r;
        });
    }

    /** "resultado = phishing, nivelRiesgo = 85, indicadores = [TLD_SOSPECHOSO, ...]" */
    static String resumenAnalisis(JsonPath json) {
        return "resultado = " + json.getString("resultado")
                + ", nivelRiesgo = " + json.getString("nivelRiesgo")
                + ", indicadores = " + codigosIndicadores(json);
    }

    static List<String> codigosIndicadores(JsonPath json) {
        List<String> codigos = json.getList("indicadores.codigo", String.class);
        return codigos == null ? List.of() : codigos;
    }

    /** codigo -> aporte, con el aporte normalizado (85.00 y 85 se comparan igual). */
    static Map<String, String> aportes(JsonPath json) {
        List<Map<String, Object>> indicadores = json.getList("indicadores");
        assertNotNull(indicadores, "La respuesta no trae la lista de indicadores");
        return indicadores.stream().collect(Collectors.toMap(
                i -> String.valueOf(i.get("codigo")),
                i -> new java.math.BigDecimal(String.valueOf(i.get("aporte"))).stripTrailingZeros().toPlainString(),
                (a, b) -> a + "+" + b,
                java.util.TreeMap::new));
    }

    static double riesgo(JsonPath json) {
        String valor = json.getString("nivelRiesgo");
        assertNotNull(valor, "La respuesta no trae nivelRiesgo");
        return Double.parseDouble(valor);
    }
}
