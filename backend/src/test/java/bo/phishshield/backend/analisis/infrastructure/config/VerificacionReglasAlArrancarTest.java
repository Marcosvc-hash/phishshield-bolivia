package bo.phishshield.backend.analisis.infrastructure.config;

import bo.phishshield.backend.analisis.application.AnalizarUrlUseCase;
import bo.phishshield.backend.analisis.domain.service.ReglaIpLiteral;
import bo.phishshield.backend.analisis.domain.service.ReglaSinHttps;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class VerificacionReglasAlArrancarTest {

    @Test
    void detecta_las_reglas_que_faltan_en_la_base() {
        List<String> faltantes = VerificacionReglasAlArrancar.codigosSinRegistrar(
                List.of("URL_SIN_HTTPS", "URL_LISTA_NEGRA", "URL_IP_LITERAL"),
                Set.of("URL_SIN_HTTPS", "URL_IP_LITERAL", "TXT_URGENCIA"));

        assertEquals(List.of("URL_LISTA_NEGRA"), faltantes);
    }

    @Test
    void no_avisa_si_todas_estan_registradas() {
        assertTrue(VerificacionReglasAlArrancar.codigosSinRegistrar(
                List.of("URL_SIN_HTTPS"), Set.of("URL_SIN_HTTPS", "URL_DOMINIO_NUEVO")).isEmpty());
    }

    @Test
    void el_caso_de_uso_incluye_las_reglas_dinamicas() {
        AnalizarUrlUseCase casoDeUso = new AnalizarUrlUseCase(
                List.of(new ReglaIpLiteral(), new ReglaSinHttps()),
                null, null, null, null, null, Clock.systemDefaultZone());

        assertEquals(
                List.of("URL_IP_LITERAL", "URL_SIN_HTTPS", "URL_TYPOSQUATTING", "URL_LISTA_NEGRA"),
                casoDeUso.codigosDeReglas());
    }
}
