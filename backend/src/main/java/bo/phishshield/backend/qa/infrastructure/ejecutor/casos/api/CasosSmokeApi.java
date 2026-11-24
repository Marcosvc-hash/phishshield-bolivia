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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Suite Smoke (funcional), casos de API: lo minimo para saber si vale la
 * pena seguir probando. Los casos de interfaz de esta suite (TC-SMK-005 y
 * TC-SMK-006) van con Playwright.
 */
@ExtendWith(ExtensionQA.class)
public class CasosSmokeApi {

    @Test
    @CasoQA("TC-SMK-001")
    void laApiDelBackendResponde(ContextoCaso ctx) {
        ctx.requiereBackend();

        Response r = ctx.paso("Enviar GET /api/auth/yo sin el encabezado Authorization", () -> {
            Response x = ctx.api().sinSesion().get("/api/auth/yo");
            ctx.observar("HTTP " + x.statusCode() + ", Content-Type " + x.contentType());
            return x;
        });
        ctx.paso("Verificar que responde HTTP 401", () -> assertEquals(401, r.statusCode(), "Código HTTP"));
        ctx.paso("Verificar que el cuerpo es el JSON de error de PhishShield (estado y mensaje)", () -> {
            assertTrue(r.contentType() != null && r.contentType().contains("json"),
                    "El cuerpo no es JSON (Content-Type " + r.contentType() + ")");
            Object estado = r.jsonPath().get("estado");
            String mensaje = r.jsonPath().getString("mensaje");
            ctx.observar("estado = " + estado + ", mensaje = \"" + mensaje + "\"");
            assertEquals(401, estado, "Campo estado del cuerpo");
            assertFalse(mensaje == null || mensaje.isBlank(), "El cuerpo no trae mensaje");
        });
    }

    @Test
    @CasoQA("TC-SMK-002")
    void loginDelAdministradorDePrueba(ContextoCaso ctx) {
        ctx.requiereSesion();

        Response r = ctx.paso("Enviar POST /api/auth/login con el correo de QA_TEST_USER y su contraseña", () -> {
            Response x = ctx.api().sinSesion()
                    .body(ClienteApiQA.json("email", ctx.entorno().usuario(), "contrasena", ctx.entorno().contrasena()))
                    .post("/api/auth/login");
            ctx.observar("HTTP " + x.statusCode());
            return x;
        });
        ctx.paso("Verificar que responde HTTP 200", () -> assertEquals(200, r.statusCode(), "Código HTTP del login"));
        ctx.paso("Verificar que trae tokenAcceso y tokenRefresco", () -> {
            String acceso = r.jsonPath().getString("tokenAcceso");
            String refresco = r.jsonPath().getString("tokenRefresco");
            ctx.observar("tokenAcceso " + (acceso == null ? "ausente" : "presente (" + acceso.length() + " caracteres)")
                    + ", tokenRefresco " + (refresco == null ? "ausente" : "presente"));
            assertFalse(acceso == null || acceso.isBlank(), "Falta tokenAcceso");
            assertFalse(refresco == null || refresco.isBlank(), "Falta tokenRefresco");
        });
        ctx.paso("Verificar que usuario.rol es ADMIN", () -> {
            String rol = r.jsonPath().getString("usuario.rol");
            ctx.observar("usuario.rol = " + rol + ", usuario.email = " + r.jsonPath().getString("usuario.email"));
            assertEquals("ADMIN", rol, "Rol del usuario de prueba");
        });
    }

    @Test
    @CasoQA("TC-SMK-003")
    void elListadoDeEmpresasEstaDisponible(ContextoCaso ctx) {
        ctx.requiereSesion();
        PasosApi.iniciarSesion(ctx);

        Response r = ctx.paso("Enviar GET /api/empresas con el token del administrador", () -> {
            Response x = ctx.api().conSesion().get("/api/empresas");
            ctx.observar("HTTP " + x.statusCode());
            return x;
        });
        ctx.paso("Verificar que responde HTTP 200", () -> assertEquals(200, r.statusCode(), "Código HTTP"));
        ctx.paso("Verificar que el cuerpo es una lista JSON", () -> {
            Object cuerpo = r.jsonPath().get("$");
            List<?> empresas = assertInstanceOf(List.class, cuerpo, "El cuerpo no es una lista");
            ctx.observar("Lista con " + empresas.size() + " empresa(s)");
        });
    }

    @Test
    @CasoQA("TC-SMK-004")
    void losDatosDelPanelEstanDisponibles(ContextoCaso ctx) {
        ctx.requiereSesion();
        PasosApi.iniciarSesion(ctx);

        Response r = ctx.paso("Enviar GET /api/admin/panel con el token del administrador", () -> {
            Response x = ctx.api().conSesion().get("/api/admin/panel");
            ctx.observar("HTTP " + x.statusCode());
            return x;
        });
        ctx.paso("Verificar que responde HTTP 200", () -> assertEquals(200, r.statusCode(), "Código HTTP"));
        ctx.paso("Verificar que trae el resumen del panel", () -> {
            Object total = r.jsonPath().get("resumen.totalAnalisis");
            ctx.observar("resumen.totalAnalisis = " + total + ", resumen.totalPhishing = "
                    + r.jsonPath().get("resumen.totalPhishing") + ", alertasAbiertas = "
                    + r.jsonPath().get("resumen.alertasAbiertas"));
            assertNotNull(total, "El panel no trae resumen.totalAnalisis");
        });
    }
}
