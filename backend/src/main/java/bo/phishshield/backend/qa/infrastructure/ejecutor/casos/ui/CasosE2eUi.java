package bo.phishshield.backend.qa.infrastructure.ejecutor.casos.ui;

import bo.phishshield.backend.qa.infrastructure.ejecutor.CasoQA;
import bo.phishshield.backend.qa.infrastructure.ejecutor.ContextoCaso;
import bo.phishshield.backend.qa.infrastructure.ejecutor.ExtensionQA;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Suite E2E (funcional): el camino completo de una deteccion como lo haria
 * una persona, de punta a punta por la interfaz: login, analisis de una URL
 * de phishing, la URL en el Historial y su alerta pendiente en Alertas.
 */
@ExtendWith(ExtensionQA.class)
public class CasosE2eUi {

    @Test
    @CasoQA("TC-E2E-001")
    void flujoCompletoLoginAnalisisHistorialYAlerta(ContextoCaso ctx) {
        ctx.requiereFrontend(true);
        String marca = ctx.marca() + "-e2e";
        String url = "http://bnb-verificacion-cuenta.xyz/login?qa=" + marca;

        Page p = PasosUi.iniciarSesion(ctx);
        PasosUi.irA(ctx, p, "/admin/analizar", "analizador de URL");

        ctx.pasoUi("Analizar la URL de phishing " + url, () -> {
            p.fill("#url-a-analizar", url);
            p.click("form.buscador-url button[type=submit]");
            assertThat(p.locator("article.resultado")).isVisible();
        });
        ctx.pasoUi("Verificar que el veredicto en pantalla no es seguro", () -> {
            Locator resultado = p.locator("article.resultado");
            String clase = String.valueOf(resultado.getAttribute("class"));
            String veredicto = p.locator("#titulo-veredicto").innerText().strip();
            ctx.observar("Veredicto: \"" + veredicto + "\" (clase " + clase + ")");
            assertFalse(clase.contains("veredicto-seguro"), "El análisis salió seguro: " + veredicto);
        });

        PasosUi.irA(ctx, p, "/admin/historial", "historial de análisis");
        ctx.pasoUi("Buscar la marca " + marca + " y verificar que la URL aparece en el Historial", () -> {
            p.fill("#filtro-q", marca);
            p.click("form.barra-filtros button[type=submit]");
            Locator fila = p.locator("table.tabla tbody td.celda-url", new Page.LocatorOptions().setHasText(marca));
            assertThat(fila.first()).isVisible();
            ctx.observar("Fila encontrada: " + fila.first().innerText().strip());
        });

        PasosUi.irA(ctx, p, "/admin/alertas", "bandeja de alertas, filtro Pendientes");
        ctx.pasoUi("Verificar que su alerta aparece en Alertas con estado Pendiente", () -> {
            Locator fila = p.locator("table.tabla tbody tr", new Page.LocatorOptions().setHasText(marca));
            assertThat(fila.first()).isVisible();
            Locator estado = fila.first().locator("td[data-etiqueta=Estado] .badge");
            assertThat(estado).hasText("Pendiente");
            ctx.observar("Alerta de " + marca + " con estado \"" + estado.innerText().strip() + "\" y severidad \""
                    + fila.first().locator("td[data-etiqueta=Severidad]").innerText().strip() + "\"");
        });
    }
}
