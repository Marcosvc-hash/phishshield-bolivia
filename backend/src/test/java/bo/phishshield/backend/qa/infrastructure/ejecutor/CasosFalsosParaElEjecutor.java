package bo.phishshield.backend.qa.infrastructure.ejecutor;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.ConnectException;
import java.io.UncheckedIOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Casos de mentira que solo usa JUnitEjecutorPruebasAdapterTest, a traves
 * del launcher. Maven no los corre como pruebas sueltas porque el nombre de
 * la clase no termina en "Test" (sin el Centro de Pruebas no tienen ContextoCaso).
 */
@ExtendWith(ExtensionQA.class)
class CasosFalsosParaElEjecutor {

    @Test
    @CasoQA("TC-TST-001")
    void pasa(ContextoCaso ctx) {
        ctx.paso("Un paso que se cumple", () -> ctx.observar("todo bien"));
    }

    @Test
    @CasoQA("TC-TST-002")
    void fallaUnaAsercion(ContextoCaso ctx) {
        ctx.paso("Paso previo", () -> { });
        ctx.paso("Verificar el código HTTP", () -> {
            ctx.observar("HTTP 500");
            assertEquals(200, 500, "Código HTTP");
        });
    }

    @Test
    @CasoQA("TC-TST-003")
    void noHayConexion(ContextoCaso ctx) {
        ctx.paso("Llamar a un servicio caído", () -> {
            throw new UncheckedIOException(new ConnectException("Connection refused"));
        });
    }

    @Test
    @CasoQA("TC-TST-004")
    void faltaConfiguracion(ContextoCaso ctx) {
        ctx.requiereSesion();
    }

    @Test
    @CasoQA("TC-TST-005")
    void precondicionNoCumplida(ContextoCaso ctx) {
        Assumptions.assumeTrue(false, "No hay datos para comprobar");
    }
}
