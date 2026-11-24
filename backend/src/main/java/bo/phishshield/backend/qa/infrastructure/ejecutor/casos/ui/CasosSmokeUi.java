package bo.phishshield.backend.qa.infrastructure.ejecutor.casos.ui;

import bo.phishshield.backend.qa.infrastructure.ejecutor.CasoQA;
import bo.phishshield.backend.qa.infrastructure.ejecutor.ContextoCaso;
import bo.phishshield.backend.qa.infrastructure.ejecutor.ExtensionQA;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Suite Smoke (funcional), casos de interfaz con Playwright: el login llega
 * al panel y la pantalla de analisis abre.
 */
@ExtendWith(ExtensionQA.class)
public class CasosSmokeUi {

    @Test
    @CasoQA("TC-SMK-005")
    void elLoginEnLaInterfazLlegaAlPanel(ContextoCaso ctx) {
        ctx.requiereFrontend(true);
        Page p = PasosUi.abrirLogin(ctx);

        ctx.pasoUi("Escribir el correo y la contraseña del administrador de prueba", () -> {
            p.fill("#email", ctx.entorno().usuario());
            p.fill("#contrasena", ctx.entorno().contrasena());
            ctx.observar("Correo " + ctx.entorno().usuario() + " y contraseña escritos (la contraseña no se registra)");
        });
        ctx.pasoUi("Pulsar Ingresar y verificar que la aplicación redirige a /admin/panel", () -> {
            p.click("form button[type=submit]");
            assertThat(p).hasURL(Pattern.compile(".*/admin/panel$"));
            ctx.observar("URL actual: " + PasosUi.ruta(p));
        });
        ctx.pasoUi("Verificar que se muestra el título \"Panel\"", () -> {
            Locator titulo = p.locator("h1.titulo");
            assertThat(titulo).hasText("Panel");
            ctx.observar("Título: " + titulo.innerText());
        });
    }

    @Test
    @CasoQA("TC-SMK-006")
    void laPantallaDeAnalisisCarga(ContextoCaso ctx) {
        ctx.requiereFrontend(true);
        Page p = PasosUi.iniciarSesion(ctx);
        PasosUi.irA(ctx, p, "/admin/analizar", "analizador de URL");

        ctx.pasoUi("Verificar que el campo para pegar la URL y el botón \"Analizar\" están visibles y habilitados", () -> {
            Locator campo = p.locator("#url-a-analizar");
            Locator boton = p.locator("form.buscador-url button[type=submit]");
            assertThat(campo).isVisible();
            assertThat(campo).isEnabled();
            assertThat(boton).isVisible();
            assertThat(boton).isEnabled();
            assertThat(boton).containsText("Analizar");
            ctx.observar("Campo visible y habilitado; botón \"" + boton.innerText().strip() + "\" visible y habilitado");
        });
    }
}
