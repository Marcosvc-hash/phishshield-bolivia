package bo.phishshield.backend.qa.infrastructure.ejecutor.casos.ui;

import bo.phishshield.backend.qa.infrastructure.ejecutor.ContextoCaso;
import com.microsoft.playwright.Page;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Pasos de interfaz que repiten varios casos (abrir el login, iniciar sesion, ir a una pantalla). */
final class PasosUi {

    private PasosUi() {
    }

    static String url(ContextoCaso ctx, String ruta) {
        return ctx.entorno().frontendUrl() + ruta;
    }

    /** Abre /login y comprueba que el formulario esta visible. */
    static Page abrirLogin(ContextoCaso ctx) {
        return ctx.pasoUi("Abrir la pantalla de login (" + url(ctx, "/login") + ")", () -> {
            Page p = ctx.navegador().pagina();
            ctx.navegador().ir(url(ctx, "/login"));
            assertThat(p.locator("#email")).isVisible();
            ctx.observar("Formulario de login visible");
            return p;
        });
    }

    /**
     * Inicia sesion con el administrador de prueba y espera llegar al panel.
     * La contrasena se escribe en el campo pero nunca en la evidencia.
     */
    static Page iniciarSesion(ContextoCaso ctx) {
        Page p = abrirLogin(ctx);
        ctx.pasoUi("Escribir el correo y la contraseña del administrador de prueba y pulsar Ingresar", () -> {
            p.fill("#email", ctx.entorno().usuario());
            p.fill("#contrasena", ctx.entorno().contrasena());
            p.click("form button[type=submit]");
            p.waitForURL(Pattern.compile(".*/admin/.*"));
            ctx.observar("Sesión iniciada; la aplicación abrió " + ruta(p));
        });
        return p;
    }

    static void irA(ContextoCaso ctx, Page p, String ruta, String descripcionPantalla) {
        ctx.pasoUi("Ir a " + ruta + " (" + descripcionPantalla + ")", () -> {
            ctx.navegador().ir(url(ctx, ruta));
            assertThat(p.locator("h1.titulo")).isVisible();
            ctx.observar("Pantalla \"" + p.locator("h1.titulo").innerText() + "\" abierta");
        });
    }

    /** La ruta de la URL actual, sin el host: /admin/panel. */
    static String ruta(Page p) {
        return p.url().replaceFirst("^https?://[^/]+", "");
    }
}
