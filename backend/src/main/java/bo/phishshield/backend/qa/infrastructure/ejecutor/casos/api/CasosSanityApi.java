package bo.phishshield.backend.qa.infrastructure.ejecutor.casos.api;

import bo.phishshield.backend.qa.infrastructure.ejecutor.CasoQA;
import bo.phishshield.backend.qa.infrastructure.ejecutor.ContextoCaso;
import bo.phishshield.backend.qa.infrastructure.ejecutor.ExtensionQA;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Suite Sanity (funcional): comprueba que el motor de deteccion sigue dando
 * veredictos razonables en tres URL representativas de Bolivia. Los
 * analisis se hacen sin sesion, como un visitante del analizador publico.
 */
@ExtendWith(ExtensionQA.class)
public class CasosSanityApi {

    @Test
    @CasoQA("TC-SAN-001")
    void urlOficialDeBancoEsSegura(ContextoCaso ctx) {
        ctx.requiereBackend();
        JsonPath r = PasosApi.analizar(ctx, "https://www.bnb.com.bo/", false).jsonPath();

        ctx.paso("Verificar que el resultado es \"seguro\"", () -> {
            ctx.observar("resultado = " + r.getString("resultado"));
            assertEquals("seguro", r.getString("resultado"), "Resultado del análisis");
        });
        ctx.paso("Verificar que el nivel de riesgo es menor a 40", () -> {
            double riesgo = PasosApi.riesgo(r);
            ctx.observar("nivelRiesgo = " + r.getString("nivelRiesgo"));
            assertTrue(riesgo < 40, "Nivel de riesgo esperado < 40, obtenido " + r.getString("nivelRiesgo"));
        });
    }

    @Test
    @CasoQA("TC-SAN-002")
    void imitacionDelBnbConExtensionAbusadaEsPhishing(ContextoCaso ctx) {
        ctx.requiereBackend();
        verificarPhishing(ctx, "http://bnb-verificacion-cuenta.xyz/login");
    }

    @Test
    @CasoQA("TC-SAN-003")
    void typosquattingDeBancoSolEsPhishing(ContextoCaso ctx) {
        ctx.requiereBackend();
        verificarPhishing(ctx, "https://bancosoI.com.bo/login");
    }

    private static void verificarPhishing(ContextoCaso ctx, String url) {
        JsonPath r = PasosApi.analizar(ctx, url, false).jsonPath();

        ctx.paso("Verificar que el resultado es \"phishing\"", () -> {
            ctx.observar(PasosApi.resumenAnalisis(r));
            assertEquals("phishing", r.getString("resultado"), "Resultado del análisis de " + url);
        });
        ctx.paso("Verificar que el nivel de riesgo es 70 o más", () -> {
            double riesgo = PasosApi.riesgo(r);
            ctx.observar("nivelRiesgo = " + r.getString("nivelRiesgo"));
            assertTrue(riesgo >= 70, "Nivel de riesgo esperado >= 70, obtenido " + r.getString("nivelRiesgo"));
        });
    }
}
