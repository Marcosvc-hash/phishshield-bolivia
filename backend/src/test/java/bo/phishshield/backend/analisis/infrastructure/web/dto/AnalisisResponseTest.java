package bo.phishshield.backend.analisis.infrastructure.web.dto;

import bo.phishshield.backend.analisis.domain.model.Analisis;
import bo.phishshield.backend.analisis.domain.model.OrigenAnalisis;
import bo.phishshield.backend.analisis.domain.model.TipoAnalisis;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AnalisisResponseTest {

    private static Analisis conFecha(OffsetDateTime fecha) {
        return Analisis.desdePersistencia(
                1L, UUID.randomUUID(), null, null, null,
                TipoAnalisis.URL, OrigenAnalisis.WEB,
                "https://ejemplo.com", "a".repeat(64), "ejemplo.com",
                BigDecimal.ZERO, 5, fecha, List.of());
    }

    @Test
    void una_fecha_en_utc_se_muestra_en_hora_de_la_paz() {
        OffsetDateTime enUtc = OffsetDateTime.of(2026, 10, 12, 2, 51, 1, 0, ZoneOffset.UTC);

        OffsetDateTime mostrada = AnalisisResponse.desde(conFecha(enUtc)).fechaAnalisis();

        assertEquals(ZoneOffset.ofHours(-4), mostrada.getOffset());
        assertEquals(OffsetDateTime.of(2026, 10, 11, 22, 51, 1, 0, ZoneOffset.ofHours(-4)), mostrada);
        assertTrue(enUtc.isEqual(mostrada), "Debe ser el mismo instante");
    }

    @Test
    void una_fecha_nula_sigue_siendo_nula() {
        assertNull(AnalisisResponse.desde(conFecha(null)).fechaAnalisis());
    }
}
