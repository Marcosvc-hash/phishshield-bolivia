package bo.phishshield.backend.qa.infrastructure.ejecutor.casos.api;

import bo.phishshield.backend.qa.infrastructure.ejecutor.CasoQA;
import bo.phishshield.backend.qa.infrastructure.ejecutor.ClienteApiQA;
import bo.phishshield.backend.qa.infrastructure.ejecutor.ContextoCaso;
import bo.phishshield.backend.qa.infrastructure.ejecutor.ExtensionQA;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Suite Regression (funcional), casos de API: comportamientos que ya
 * funcionaban y no deben romperse. Los casos de interfaz de esta suite
 * (TC-REG-001, 002, 004 y 007) van con Playwright.
 */
@ExtendWith(ExtensionQA.class)
public class CasosRegressionApi {

    private static final String URL_MALFORMADA = "esto no es una url";

    @Test
    @CasoQA("TC-REG-003")
    void laApiRechazaUnaUrlMalformada(ContextoCaso ctx) {
        ctx.requiereSesion();
        PasosApi.iniciarSesion(ctx);

        long antes = ctx.paso("Contar en el historial los análisis con el texto \"" + URL_MALFORMADA + "\"",
                () -> contarEnHistorial(ctx, URL_MALFORMADA));

        Response r = ctx.paso("Enviar POST /api/analisis con la URL \"" + URL_MALFORMADA + "\"", () -> {
            Response x = ctx.api().sinSesion()
                    .body(ClienteApiQA.json("url", URL_MALFORMADA, "origen", "api"))
                    .post("/api/analisis");
            ctx.observar("HTTP " + x.statusCode());
            return x;
        });
        ctx.paso("Verificar que responde HTTP 400 con un mensaje de error de PhishShield", () -> {
            String mensaje = r.jsonPath().getString("mensaje");
            ctx.observar("HTTP " + r.statusCode() + ", mensaje = \"" + mensaje + "\"");
            assertEquals(400, r.statusCode(), "Código HTTP");
            assertFalse(mensaje == null || mensaje.isBlank(), "La respuesta no trae mensaje");
        });
        ctx.paso("Verificar que no se creó ningún análisis", () -> {
            long despues = contarEnHistorial(ctx, URL_MALFORMADA);
            ctx.observar("Antes: " + antes + ", después: " + despues);
            assertEquals(antes, despues, "Cantidad de análisis con ese texto");
        });
    }

    private static long contarEnHistorial(ContextoCaso ctx, String texto) {
        Response r = ctx.api().conSesion().queryParam("q", texto).get("/api/admin/analisis");
        assertEquals(200, r.statusCode(), "Código HTTP del historial");
        long total = r.jsonPath().getLong("totalElementos");
        ctx.observar(total + " análisis");
        return total;
    }

    @Test
    @CasoQA("TC-REG-005")
    void gestionDeEmpresas(ContextoCaso ctx) {
        ctx.requiereSesion();
        PasosApi.iniciarSesion(ctx);
        String nombre = "QA Empresa " + ctx.marca();

        String id = ctx.paso("Crear la empresa \"" + nombre + "\" con POST /api/empresas", () -> {
            Response x = ctx.api().conSesion()
                    .body(ClienteApiQA.json("nombre", nombre, "nit", null, "sector", "Pruebas QA",
                            "ciudad", "Cochabamba", "plan", "basico"))
                    .post("/api/empresas");
            ctx.observar("HTTP " + x.statusCode() + ", activa = " + x.jsonPath().get("activa"));
            assertEquals(201, x.statusCode(), "Código HTTP al crear");
            assertEquals(Boolean.TRUE, x.jsonPath().get("activa"), "La empresa nueva debe quedar activa");
            return x.jsonPath().getString("id");
        });

        ctx.paso("Verificar que aparece en GET /api/empresas", () -> {
            Response x = ctx.api().conSesion().get("/api/empresas");
            List<String> ids = x.jsonPath().getList("id", String.class);
            ctx.observar("HTTP " + x.statusCode() + ", " + ids.size() + " empresa(s); la nueva "
                    + (ids.contains(id) ? "aparece" : "NO aparece"));
            assertEquals(200, x.statusCode(), "Código HTTP del listado");
            assertTrue(ids.contains(id), "La empresa creada no aparece en el listado");
        });

        ctx.paso("Editar la ciudad a \"La Paz\" con PUT /api/empresas/{id}", () -> {
            Response x = ctx.api().conSesion()
                    .body(ClienteApiQA.json("nombre", nombre, "nit", null, "sector", "Pruebas QA",
                            "ciudad", "La Paz", "plan", "basico"))
                    .put("/api/empresas/" + id);
            ctx.observar("HTTP " + x.statusCode() + ", ciudad = " + x.jsonPath().getString("ciudad"));
            assertEquals(200, x.statusCode(), "Código HTTP al editar");
            assertEquals("La Paz", x.jsonPath().getString("ciudad"), "Ciudad después de editar");
        });

        ctx.paso("Desactivar la empresa con PATCH /api/empresas/{id}/estado (el sistema no borra empresas)", () -> {
            Response x = ctx.api().conSesion()
                    .body(ClienteApiQA.json("activo", false))
                    .patch("/api/empresas/" + id + "/estado");
            ctx.observar("HTTP " + x.statusCode() + ", activa = " + x.jsonPath().get("activa"));
            assertEquals(200, x.statusCode(), "Código HTTP al desactivar");
            assertEquals(Boolean.FALSE, x.jsonPath().get("activa"), "La empresa debe quedar inactiva");
        });
    }

    @Test
    @CasoQA("TC-REG-006")
    void filtroDelHistorialPorResultado(ContextoCaso ctx) {
        ctx.requiereSesion();
        PasosApi.iniciarSesion(ctx);

        ctx.paso("Preparar: asegurar al menos un análisis phishing en la base", () -> {
            Response x = ctx.api().conSesion().queryParam("resultado", "phishing").queryParam("tamano", 1)
                    .get("/api/admin/analisis");
            assertEquals(200, x.statusCode(), "Código HTTP del historial");
            long hay = x.jsonPath().getLong("totalElementos");
            if (hay > 0) {
                ctx.observar("Ya hay " + hay + " análisis phishing");
                return;
            }
            String url = "http://bnb-bo-seguro.com/login/verificar-cuenta?qa=" + ctx.marca();
            Response nuevo = ctx.api().conSesion().body(ClienteApiQA.json("url", url, "origen", "api"))
                    .post("/api/analisis");
            ctx.observar("No había; se analizó " + url + " -> " + nuevo.jsonPath().getString("resultado"));
        });

        Response r = ctx.paso("Enviar GET /api/admin/analisis?resultado=phishing&tamano=100", () -> {
            Response x = ctx.api().conSesion().queryParam("resultado", "phishing").queryParam("tamano", 100)
                    .get("/api/admin/analisis");
            ctx.observar("HTTP " + x.statusCode() + ", totalElementos = " + x.jsonPath().get("totalElementos"));
            assertEquals(200, x.statusCode(), "Código HTTP");
            return x;
        });
        ctx.paso("Verificar que todas las filas devueltas tienen resultado \"phishing\"", () -> {
            List<String> resultados = r.jsonPath().getList("contenido.resultado", String.class);
            List<String> otros = resultados.stream().filter(s -> !"phishing".equals(s)).distinct().toList();
            ctx.observar(resultados.size() + " fila(s) revisadas; resultados distintos de phishing: " + otros);
            assertFalse(resultados.isEmpty(), "El filtro no devolvió filas aunque hay análisis phishing");
            assertTrue(otros.isEmpty(), "El filtro devolvió filas con otros resultados: " + otros);
        });
    }

    @Test
    @CasoQA("TC-REG-008")
    void rutasAdministrativasSinTokenDevuelven401(ContextoCaso ctx) {
        ctx.requiereBackend();
        for (String ruta : List.of("/api/empresas", "/api/admin/panel", "/api/admin/qa/casos")) {
            ctx.paso("Enviar GET " + ruta + " sin el encabezado Authorization y verificar HTTP 401", () -> {
                Response x = ctx.api().sinSesion().get(ruta);
                ctx.observar("HTTP " + x.statusCode());
                assertEquals(401, x.statusCode(), "Código HTTP de " + ruta + " sin token");
            });
        }
    }
}
