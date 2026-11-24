package bo.phishshield.backend.qa.infrastructure.ejecutor.casos.api;

import bo.phishshield.backend.qa.infrastructure.ejecutor.CasoQA;
import bo.phishshield.backend.qa.infrastructure.ejecutor.ContextoCaso;
import bo.phishshield.backend.qa.infrastructure.ejecutor.ExtensionQA;
import bo.phishshield.backend.qa.infrastructure.ejecutor.FalloInfraestructuraException;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Suite Integration (funcional): el backend con sus vecinos, es decir, el
 * servicio de IA (Python) y PostgreSQL. Las URL llevan la marca de la
 * ejecucion (?qa=...) para que no salgan de la cache de 24 h y se puedan
 * encontrar despues en la base.
 */
@ExtendWith(ExtensionQA.class)
public class CasosIntegrationApi {

    /** Host fuera de la lista blanca que el modelo marca como phishing (se puede cambiar con el parametro urlIa). */
    static final String URL_IA_POR_DEFECTO = "http://bnb-verificacion-cuenta.xyz/login";

    /** Dominio de la lista negra: el analisis nunca es seguro y siempre genera alerta. */
    static final String URL_LISTA_NEGRA = "http://bnb-bo-seguro.com/login/verificar-cuenta";

    @Test
    @CasoQA("TC-INT-001")
    void integracionDelBackendConElServicioDeIa(ContextoCaso ctx) {
        ctx.requiereBackend();
        ctx.requiereServicioIa();
        String iaUrl = ctx.entorno().iaUrl();

        ctx.paso("Enviar GET " + iaUrl + "/salud al servicio de IA", () -> {
            Response x;
            try {
                x = ctx.api().hacia(iaUrl).get("/salud");
            } catch (RuntimeException ex) {
                throw new FalloInfraestructuraException("El servicio de IA no responde en " + iaUrl
                        + " (¿está levantado? ia/servicio.py)", ex);
            }
            ctx.observar("HTTP " + x.statusCode() + ", " + x.asString());
            if (x.statusCode() != 200) {
                throw new FalloInfraestructuraException("El servicio de IA respondió HTTP " + x.statusCode());
            }
        });

        String url = ctx.parametro("urlIa", URL_IA_POR_DEFECTO) + "?qa=" + ctx.marca() + "-int001";
        JsonPath r = PasosApi.analizar(ctx, url, false).jsonPath();

        ctx.paso("Verificar que el análisis incluye el indicador IA_MODELO_URL", () -> {
            List<String> codigos = PasosApi.codigosIndicadores(r);
            ctx.observar("Indicadores: " + codigos);
            assertTrue(codigos.contains("IA_MODELO_URL"),
                    "El análisis no trae IA_MODELO_URL: el backend no usó la respuesta del modelo. Indicadores: " + codigos);
        });
    }

    @Test
    @CasoQA("TC-INT-002")
    void elAnalisisQuedaGuardadoEnPostgresql(ContextoCaso ctx) {
        ctx.requiereSesion();
        PasosApi.iniciarSesion(ctx);

        String url = URL_LISTA_NEGRA + "?qa=" + ctx.marca() + "-int002";
        JsonPath analisis = PasosApi.analizar(ctx, url, true).jsonPath();
        String id = analisis.getString("id");

        JsonPath guardado = ctx.paso("Leer el análisis desde la base con GET /api/admin/analisis/" + id, () -> {
            Response x = ctx.api().conSesion().get("/api/admin/analisis/" + id);
            ctx.observar("HTTP " + x.statusCode());
            assertEquals(200, x.statusCode(), "Código HTTP del detalle");
            return x.jsonPath();
        });

        ctx.paso("Verificar que la URL, el resultado y el nivel de riesgo coinciden", () -> {
            ctx.observar("Respuesta: " + analisis.getString("url") + " / " + analisis.getString("resultado") + " / "
                    + analisis.getString("nivelRiesgo") + " | Base: " + guardado.getString("url") + " / "
                    + guardado.getString("resultado") + " / " + guardado.getString("nivelRiesgo"));
            assertEquals(analisis.getString("url"), guardado.getString("url"), "URL guardada");
            assertEquals(analisis.getString("resultado"), guardado.getString("resultado"), "Resultado guardado");
            assertEquals(0, new BigDecimal(analisis.getString("nivelRiesgo"))
                            .compareTo(new BigDecimal(guardado.getString("nivelRiesgo"))),
                    "Nivel de riesgo guardado: " + guardado.getString("nivelRiesgo")
                            + " (la respuesta decía " + analisis.getString("nivelRiesgo") + ")");
        });

        ctx.paso("Verificar que los indicadores y sus aportes coinciden", () -> {
            Map<String, String> enRespuesta = PasosApi.aportes(analisis);
            Map<String, String> enBase = PasosApi.aportes(guardado);
            ctx.observar("Respuesta: " + enRespuesta + " | Base: " + enBase);
            assertEquals(enRespuesta, enBase, "Indicadores guardados (código = aporte)");
        });
    }

    @Test
    @CasoQA("TC-INT-003")
    void unAnalisisNoSeguroGeneraSuAlerta(ContextoCaso ctx) {
        ctx.requiereSesion();
        PasosApi.iniciarSesion(ctx);

        String url = URL_LISTA_NEGRA + "?qa=" + ctx.marca() + "-int003";
        JsonPath analisis = PasosApi.analizar(ctx, url, true).jsonPath();
        String id = analisis.getString("id");

        ctx.paso("Verificar que el análisis no es seguro y requiere alerta", () -> {
            ctx.observar("resultado = " + analisis.getString("resultado") + ", requiereAlerta = "
                    + analisis.get("requiereAlerta"));
            assertNotEquals("seguro", analisis.getString("resultado"), "Resultado del análisis");
            assertEquals(Boolean.TRUE, analisis.get("requiereAlerta"), "requiereAlerta");
        });

        ctx.paso("Buscar en GET /api/admin/alertas?estado=pendiente una alerta con idAnalisis = " + id, () -> {
            Response x = ctx.api().conSesion().queryParam("estado", "pendiente").queryParam("tamano", 100)
                    .get("/api/admin/alertas");
            assertEquals(200, x.statusCode(), "Código HTTP de la bandeja de alertas");
            List<Map<String, Object>> alertas = x.jsonPath().getList("contenido");
            Map<String, Object> suya = alertas.stream()
                    .filter(a -> id.equals(String.valueOf(a.get("idAnalisis"))))
                    .findFirst()
                    .orElse(null);
            ctx.observar(alertas.size() + " alerta(s) pendientes revisadas; "
                    + (suya == null ? "ninguna es del análisis" : "alerta " + suya.get("id") + ", severidad "
                    + suya.get("severidad") + ", estado " + suya.get("estado")));
            assertTrue(suya != null, "No hay una alerta pendiente para el análisis " + id);
            assertEquals("pendiente", suya.get("estado"), "Estado de la alerta");
        });
    }
}
