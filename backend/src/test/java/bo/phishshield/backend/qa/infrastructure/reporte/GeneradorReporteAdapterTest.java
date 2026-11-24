package bo.phishshield.backend.qa.infrastructure.reporte;

import bo.phishshield.backend.qa.domain.model.AlcanceEjecucion;
import bo.phishshield.backend.qa.domain.model.ArchivosReporte;
import bo.phishshield.backend.qa.domain.model.CasoPrueba;
import bo.phishshield.backend.qa.domain.model.CorreoDestino;
import bo.phishshield.backend.qa.domain.model.DesenlaceCaso;
import bo.phishshield.backend.qa.domain.model.Ejecucion;
import bo.phishshield.backend.qa.domain.model.EstadoPaso;
import bo.phishshield.backend.qa.domain.model.Herramienta;
import bo.phishshield.backend.qa.domain.model.MetricaRendimiento;
import bo.phishshield.backend.qa.domain.model.PasoEjecutado;
import bo.phishshield.backend.qa.domain.model.Suite;
import bo.phishshield.backend.qa.domain.model.TipoFallo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Genera los reportes de verdad en una carpeta temporal y revisa su contenido. */
class GeneradorReporteAdapterTest {

    private static final OffsetDateTime INICIO = OffsetDateTime.parse("2026-11-24T10:00:00-04:00");

    @TempDir
    Path temporal;

    private static CasoPrueba caso(String codigo, int orden, Herramienta herramienta) {
        return new CasoPrueba((long) orden, codigo, Suite.SMOKE, herramienta, orden, "Caso " + codigo,
                "Objetivo de " + codigo, "Precondiciones", "Datos de prueba", "Resultado esperado", true);
    }

    /** Ejecucion con un PASS (captura presente), un FAIL (captura faltante y metrica) y un NOT_RUN. */
    private Ejecucion ejecucion(UUID id) throws IOException {
        Path capturas = temporal.resolve(id.toString()).resolve("capturas");
        Files.createDirectories(capturas);
        ImageIO.write(new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB), "png",
                capturas.resolve("TC-SMK-005-01.png").toFile());

        Ejecucion e = new Ejecucion(1L, id, 1L, Suite.SMOKE, AlcanceEjecucion.SUITE_COMPLETA,
                new CorreoDestino("qa@phishshield.bo"), "phishshield_qa", Map.of("usuarios", "5"),
                bo.phishshield.backend.qa.domain.model.EstadoEjecucion.PENDIENTE, null, INICIO, null, null, null,
                bo.phishshield.backend.qa.domain.model.EstadoCorreo.PENDIENTE, null, null, null, null,
                List.of(
                        bo.phishshield.backend.qa.domain.model.ResultadoCaso.pendiente(caso("TC-SMK-001", 1, Herramienta.API), 1),
                        bo.phishshield.backend.qa.domain.model.ResultadoCaso.pendiente(caso("TC-SMK-005", 2, Herramienta.UI), 2),
                        bo.phishshield.backend.qa.domain.model.ResultadoCaso.pendiente(caso("TC-SMK-006", 3, Herramienta.UI), 3)));
        e.iniciar(INICIO);
        e.registrarResultado(DesenlaceCaso.aprobado("TC-SMK-001", INICIO, INICIO.plusSeconds(1),
                List.of(new PasoEjecutado(1, "Enviar GET /api/auth/yo", "HTTP 401", EstadoPaso.OK, null,
                        "GET http://localhost:8080/api/auth/yo\n-> HTTP 401 (12 ms)", INICIO)), List.of()));
        e.registrarResultado(DesenlaceCaso.fallido("TC-SMK-005", TipoFallo.FUNCIONAL,
                "Se esperaba <script>alert(1)</script> en el título", "Abrir el panel", INICIO, INICIO.plusSeconds(3),
                List.of(new PasoEjecutado(1, "Iniciar sesión", "Login correcto", EstadoPaso.OK,
                                id + "/capturas/TC-SMK-005-01.png", null, INICIO),
                        new PasoEjecutado(2, "Abrir el panel", "Título distinto", EstadoPaso.FALLO,
                                id + "/capturas/TC-SMK-005-02.png", null, INICIO)),
                List.of(MetricaRendimiento.conMaximo("p95", new BigDecimal("2500"), "ms", new BigDecimal("2000")))));
        e.anotarNoEjecutado("TC-SMK-006", "Falta configurar QA_FRONTEND_URL");
        e.finalizar(INICIO.plusSeconds(5));
        return e;
    }

    @Test
    void genera_el_pdf_y_el_html_aunque_falte_una_captura() throws IOException {
        UUID id = UUID.randomUUID();
        CarpetaEvidencias carpeta = new CarpetaEvidencias(temporal);

        ArchivosReporte archivos = new GeneradorReporteAdapter(carpeta, Clock.systemDefaultZone()).generar(ejecucion(id));

        assertEquals(id + "/reporte-detallado.pdf", archivos.rutaPdf());
        assertEquals(id + "/reporte-resumen.html", archivos.rutaHtml());

        byte[] pdf = Files.readAllBytes(temporal.resolve(archivos.rutaPdf()));
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
        String texto = textoDelPdf(pdf);
        assertTrue(texto.contains("Reporte de automatización de pruebas"), "Portada");
        assertTrue(texto.contains("Resumen por tipo y suite"));
        assertTrue(texto.contains("TC-SMK-005"));
        assertTrue(texto.contains("Captura no disponible"), "Avisa la captura que falta");
        assertTrue(texto.contains("No cumple"), "Evalúa la métrica contra su umbral");
        assertTrue(texto.contains("Falta configurar QA_FRONTEND_URL"), "Motivo del NOT_RUN");

        String html = Files.readString(temporal.resolve(archivos.rutaHtml()));
        assertTrue(html.startsWith("<!doctype html>"));
        assertTrue(html.contains("TC-SMK-001") && html.contains("TC-SMK-006"));
        assertTrue(html.contains("Con fallos"), "Resultado global");
        assertFalse(html.contains("<script>"), "El mensaje de error se escapa");
        assertTrue(html.contains("&lt;script&gt;"));
        assertFalse(html.contains("http://") && html.contains("<link"), "Sin recursos externos");
    }

    @Test
    void la_carpeta_no_deja_salir_con_rutas_relativas() {
        CarpetaEvidencias carpeta = new CarpetaEvidencias(temporal);

        assertTrue(carpeta.resolver("abc/reporte.pdf").isPresent());
        assertTrue(carpeta.resolver("../fuera.txt").isEmpty());
        assertTrue(carpeta.resolver("abc/../../fuera.txt").isEmpty());
        assertTrue(carpeta.resolver("").isEmpty());
        assertTrue(new EvidenciasArchivoAdapter(carpeta).leer("../pom.xml").isEmpty());
    }

    private static String textoDelPdf(byte[] pdf) throws IOException {
        PdfReader lector = new PdfReader(pdf);
        StringBuilder texto = new StringBuilder();
        PdfTextExtractor extractor = new PdfTextExtractor(lector);
        for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
            texto.append(extractor.getTextFromPage(pagina)).append('\n');
        }
        lector.close();
        return texto.toString();
    }
}
