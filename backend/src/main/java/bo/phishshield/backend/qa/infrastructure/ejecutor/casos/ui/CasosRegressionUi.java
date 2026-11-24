package bo.phishshield.backend.qa.infrastructure.ejecutor.casos.ui;

import bo.phishshield.backend.qa.infrastructure.ejecutor.CasoQA;
import bo.phishshield.backend.qa.infrastructure.ejecutor.ClienteApiQA;
import bo.phishshield.backend.qa.infrastructure.ejecutor.ContextoCaso;
import bo.phishshield.backend.qa.infrastructure.ejecutor.EntornoQA;
import bo.phishshield.backend.qa.infrastructure.ejecutor.ExtensionQA;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Suite Regression (funcional), casos de interfaz con Playwright: las
 * validaciones de la pantalla de analisis, el aviso de credenciales
 * invalidas y el filtro por texto del historial.
 */
@ExtendWith(ExtensionQA.class)
public class CasosRegressionUi {

    @Test
    @CasoQA("TC-REG-001")
    void urlVaciaMuestraMensajeDeValidacion(ContextoCaso ctx) {
        ctx.requiereFrontend(true);
        Page p = PasosUi.iniciarSesion(ctx);
        PasosUi.irA(ctx, p, "/admin/analizar", "analizador de URL");

        ctx.pasoUi("Pulsar \"Analizar\" con el campo vacío", () -> {
            p.fill("#url-a-analizar", "");
            p.click("form.buscador-url button[type=submit]");
        });
        verificarValidacion(ctx, p, "Escriba una URL para analizar.");
    }

    @Test
    @CasoQA("TC-REG-002")
    void urlSinEsquemaMuestraMensajeDeValidacion(ContextoCaso ctx) {
        ctx.requiereFrontend(true);
        Page p = PasosUi.iniciarSesion(ctx);
        PasosUi.irA(ctx, p, "/admin/analizar", "analizador de URL");

        ctx.pasoUi("Escribir bnb.com.bo (sin http://) y pulsar \"Analizar\"", () -> {
            p.fill("#url-a-analizar", "bnb.com.bo");
            p.click("form.buscador-url button[type=submit]");
        });
        verificarValidacion(ctx, p, "http:// o https://");
    }

    private static void verificarValidacion(ContextoCaso ctx, Page p, String textoEsperado) {
        ctx.pasoUi("Verificar el mensaje de validación y que no aparece ningún resultado", () -> {
            Locator error = p.locator("#error-url");
            assertThat(error).isVisible();
            assertThat(error).containsText(textoEsperado);
            ctx.observar("Mensaje: \"" + error.innerText().strip() + "\"; resultados en pantalla: "
                    + p.locator("article.resultado").count());
            assertThat(p.locator("article.resultado")).hasCount(0);
        });
    }

    @Test
    @CasoQA("TC-REG-004")
    void credencialesInvalidasMuestranMensajeDeError(ContextoCaso ctx) {
        ctx.requiereFrontend(false);
        Page p = PasosUi.abrirLogin(ctx);

        ctx.pasoUi("Escribir qa.no.existe@phishshield.bo con una contraseña cualquiera y pulsar Ingresar", () -> {
            p.fill("#email", "qa.no.existe@phishshield.bo");
            p.fill("#contrasena", "clave-que-no-existe-" + ctx.marca());
            p.click("form button[type=submit]");
        });
        ctx.pasoUi("Verificar que aparece un mensaje de error y que la aplicación sigue en /login", () -> {
            Locator error = p.locator("form .aviso.error");
            assertThat(error).isVisible();
            assertThat(p).hasURL(Pattern.compile(".*/login(\\?.*)?$"));
            ctx.observar("Mensaje: \"" + error.innerText().strip() + "\"; URL: " + PasosUi.ruta(p));
        });
    }

    @Test
    @CasoQA("TC-REG-007")
    void filtroDelHistorialPorTextoEnLaInterfaz(ContextoCaso ctx) {
        ctx.requiereFrontend(true);

        ctx.paso("Preparar: asegurar un análisis de un dominio con \"bnb\" (por API)", () -> {
            if (EntornoQA.vacio(ctx.entorno().baseUrl())) {
                ctx.observar("Sin QA_BASE_URL: se usan los análisis que ya existan");
                return;
            }
            Response r = ctx.api().sinSesion()
                    .body(ClienteApiQA.json("url", "https://www.bnb.com.bo/", "origen", "api"))
                    .post("/api/analisis");
            ctx.observar("Análisis de https://www.bnb.com.bo/ -> HTTP " + r.statusCode());
        });

        Page p = PasosUi.iniciarSesion(ctx);
        PasosUi.irA(ctx, p, "/admin/historial", "historial de análisis");

        ctx.pasoUi("Buscar el texto bnb", () -> {
            p.fill("#filtro-q", "bnb");
            p.click("form.barra-filtros button[type=submit]");
            assertThat(p.locator(".chip-aplicado").first()).containsText("bnb");
            ctx.observar("Filtro aplicado: " + p.locator(".chip-aplicado").first().innerText().strip());
        });
        ctx.pasoUi("Verificar que todas las filas visibles contienen \"bnb\"", () -> {
            Locator celdas = p.locator("table.tabla tbody td.celda-url");
            assertThat(celdas.first()).isVisible();
            List<String> urls = celdas.allInnerTexts();
            List<String> sinBnb = urls.stream()
                    .filter(u -> !u.toLowerCase(Locale.ROOT).contains("bnb"))
                    .toList();
            ctx.observar(urls.size() + " fila(s) visibles; sin \"bnb\": " + sinBnb);
            assertFalse(urls.isEmpty(), "El historial filtrado no muestra filas");
            assertTrue(sinBnb.isEmpty(), "Filas que no contienen bnb: " + sinBnb);
        });
    }
}
