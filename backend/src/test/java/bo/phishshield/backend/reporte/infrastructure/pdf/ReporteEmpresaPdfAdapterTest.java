package bo.phishshield.backend.reporte.infrastructure.pdf;

import bo.phishshield.backend.reporte.domain.model.EmpresaDelReporte;
import bo.phishshield.backend.reporte.domain.model.PeriodoReporte;
import bo.phishshield.backend.reporte.domain.model.ReporteEmpresa;
import bo.phishshield.backend.reporte.domain.model.TotalesReporte;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Genera el PDF de un reporte de empresa de verdad y lee su texto. */
class ReporteEmpresaPdfAdapterTest {

    private static ReporteEmpresa reporte(TotalesReporte totales) {
        return new ReporteEmpresa(1L, UUID.randomUUID(),
                new EmpresaDelReporte(1L, UUID.randomUUID(), "Importadora Andina S.R.L.", true),
                new PeriodoReporte(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 24)), totales,
                "admin@phishshield.bo", OffsetDateTime.parse("2026-11-24T10:30:00-04:00"));
    }

    @Test
    void el_pdf_trae_la_empresa_el_periodo_y_los_totales() throws IOException {
        byte[] pdf = new ReporteEmpresaPdfAdapter().generar(reporte(new TotalesReporte(40, 6, 9)));

        assertEquals("%PDF", new String(pdf, 0, 4));
        PdfReader lector = new PdfReader(pdf);
        String texto = new PdfTextExtractor(lector).getTextFromPage(1);
        lector.close();
        assertTrue(texto.contains("Importadora Andina S.R.L."));
        assertTrue(texto.contains("01/11/2026 al 24/11/2026"));
        assertTrue(texto.contains("Análisis realizados"));
        assertTrue(texto.contains("15,0 %"), "6 de 40 es 15 %");
        assertTrue(texto.contains("admin@phishshield.bo"));
    }

    @Test
    void sin_analisis_lo_dice_en_la_lectura() {
        assertTrue(ReporteEmpresaPdfAdapter.lectura(reporte(new TotalesReporte(0, 0, 0)))
                .startsWith("En este período no se registraron análisis"));
    }
}
