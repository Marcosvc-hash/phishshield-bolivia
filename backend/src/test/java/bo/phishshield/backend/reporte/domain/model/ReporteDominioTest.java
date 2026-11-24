package bo.phishshield.backend.reporte.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

/** Reglas del periodo y de los totales de un reporte por empresa (HU-17). */
class ReporteDominioTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 24);

    @Test
    void el_periodo_incluye_los_dos_dias_y_se_cuenta_en_hora_de_bolivia() {
        PeriodoReporte p = PeriodoReporte.cerrado(LocalDate.of(2026, 11, 1), HOY, HOY);

        assertEquals(24, p.dias());
        assertEquals(OffsetDateTime.of(2026, 11, 1, 0, 0, 0, 0, ZoneOffset.ofHours(-4)), p.inicio());
        assertEquals(OffsetDateTime.of(2026, 11, 25, 0, 0, 0, 0, ZoneOffset.ofHours(-4)), p.finExclusivo());
    }

    @Test
    void un_solo_dia_es_un_periodo_valido() {
        assertEquals(1, new PeriodoReporte(HOY, HOY).dias());
    }

    @Test
    void el_periodo_rechaza_fechas_faltantes_invertidas_futuras_o_de_mas_de_un_anio() {
        assertThrows(PeriodoInvalidoException.class, () -> new PeriodoReporte(null, HOY));
        assertThrows(PeriodoInvalidoException.class, () -> new PeriodoReporte(HOY, HOY.minusDays(1)));
        assertThrows(PeriodoInvalidoException.class, () -> PeriodoReporte.cerrado(HOY, HOY.plusDays(1), HOY));
        assertThrows(PeriodoInvalidoException.class, () -> new PeriodoReporte(HOY.minusDays(366), HOY));
        assertDoesNotThrow(() -> new PeriodoReporte(HOY.minusDays(365), HOY), "366 días es el máximo");
    }

    @Test
    void los_totales_siguen_las_reglas_de_la_tabla() {
        assertEquals(new BigDecimal("33.3"), new TotalesReporte(3, 1, 2).porcentajePhishing());
        assertEquals(new BigDecimal("0.0"), new TotalesReporte(0, 0, 0).porcentajePhishing());
        assertThrows(IllegalArgumentException.class, () -> new TotalesReporte(1, 2, 0), "más phishing que análisis");
        assertThrows(IllegalArgumentException.class, () -> new TotalesReporte(-1, 0, 0));
    }
}
