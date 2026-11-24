package bo.phishshield.backend.qa.infrastructure.reporte;

import bo.phishshield.backend.qa.domain.model.Ejecucion;
import bo.phishshield.backend.qa.domain.model.EstadoCaso;
import bo.phishshield.backend.qa.domain.model.Herramienta;
import bo.phishshield.backend.qa.domain.model.MetricaRendimiento;
import bo.phishshield.backend.qa.domain.model.PasoEjecutado;
import bo.phishshield.backend.qa.domain.model.ResultadoCaso;
import bo.phishshield.backend.qa.domain.model.ResumenEjecucion;
import bo.phishshield.backend.qa.domain.model.TipoFallo;
import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reporte PDF DETALLADO de una ejecucion, con OpenPDF:
 *
 *   1. Portada: suite, tipo, ambiente, fechas, duracion y resultado global.
 *   2. Resumen por tipo y suite, por herramienta y por tipo de fallo.
 *   3. Tabla de casos con su estado y observacion.
 *   4. Metricas de las pruebas no funcionales (si las hay).
 *   5. Detalle de cada caso: metadatos del catalogo, pasos, evidencia de API
 *      y capturas. Si una captura falta, se indica en su paso y el reporte
 *      se genera igual.
 */
final class ReportePdf {

    private static final Color MARINO = new Color(0x0f1f3d);
    private static final Color PRIMARIO = new Color(0x1d4ed8);
    private static final Color TEXTO = new Color(0x0f172a);
    private static final Color TEXTO_2 = new Color(0x475569);
    private static final Color BORDE = new Color(0xe2e8f0);
    private static final Color SUPERFICIE_2 = new Color(0xf1f5f9);
    private static final Color BLANCO = Color.WHITE;

    private static final Font TITULO = new Font(Font.HELVETICA, 24, Font.BOLD, BLANCO);
    private static final Font SUBTITULO = new Font(Font.HELVETICA, 12, Font.NORMAL, new Color(0xbfdbfe));
    private static final Font H1 = new Font(Font.HELVETICA, 15, Font.BOLD, MARINO);
    private static final Font H2 = new Font(Font.HELVETICA, 11, Font.BOLD, MARINO);
    private static final Font NORMAL = new Font(Font.HELVETICA, 9, Font.NORMAL, TEXTO);
    private static final Font NEGRITA = new Font(Font.HELVETICA, 9, Font.BOLD, TEXTO);
    private static final Font ETIQUETA = new Font(Font.HELVETICA, 8, Font.BOLD, TEXTO_2);
    private static final Font PEQUENA = new Font(Font.HELVETICA, 8, Font.NORMAL, TEXTO_2);
    private static final Font ENCABEZADO_TABLA = new Font(Font.HELVETICA, 8, Font.BOLD, BLANCO);
    private static final Font MONO = new Font(Font.COURIER, 7, Font.NORMAL, TEXTO);

    /** Lo que entra de la evidencia de API en el PDF; el detalle completo esta en la pantalla. */
    private static final int MAXIMO_DETALLE_API = 2500;

    private final CarpetaEvidencias carpeta;

    ReportePdf(CarpetaEvidencias carpeta) {
        this.carpeta = carpeta;
    }

    byte[] generar(Ejecucion e, OffsetDateTime generadoEn) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 48, 48);
        PdfWriter escritor = PdfWriter.getInstance(doc, salida);
        escritor.setPageEvent(new PiePagina(e));
        doc.addTitle("Reporte de automatización - " + e.getSuite().etiqueta());
        doc.addAuthor("PhishShield Bolivia - Centro de Pruebas QA");
        doc.open();

        portada(doc, e, generadoEn);
        doc.newPage();
        resumen(doc, e);
        tablaCasos(doc, e);
        metricas(doc, e);
        for (ResultadoCaso r : e.getResultados()) {
            doc.newPage();
            detalleCaso(doc, r);
        }

        doc.close();
        return salida.toByteArray();
    }

    // ------------------------------------------------------------------
    // 1. Portada
    // ------------------------------------------------------------------

    private void portada(Document doc, Ejecucion e, OffsetDateTime generadoEn) {
        PdfPTable banda = new PdfPTable(1);
        banda.setWidthPercentage(100);
        PdfPCell celda = new PdfPCell();
        celda.setBackgroundColor(MARINO);
        celda.setBorder(Rectangle.NO_BORDER);
        celda.setPadding(28);
        celda.addElement(new Paragraph("PhishShield Bolivia · Centro de Pruebas QA", SUBTITULO));
        Paragraph titulo = new Paragraph("Reporte de automatización de pruebas", TITULO);
        titulo.setSpacingBefore(8);
        celda.addElement(titulo);
        Paragraph suite = new Paragraph("Suite " + e.getSuite().etiqueta() + " · " + e.tipo().etiqueta(), SUBTITULO);
        suite.setSpacingBefore(6);
        celda.addElement(suite);
        banda.addCell(celda);
        doc.add(banda);

        Paragraph resultado = new Paragraph();
        resultado.setSpacingBefore(24);
        resultado.add(new Chunk("Resultado global: ", H1));
        resultado.add(new Chunk(FormatoTexto.resultadoGlobal(e.getResultadoGlobal(), e.getEstado()),
                new Font(Font.HELVETICA, 15, Font.BOLD, color(FormatoTexto.colorResultado(e.getResultadoGlobal())))));
        doc.add(resultado);

        doc.add(conteos(e.resumen()));

        PdfPTable datos = new PdfPTable(new float[]{1.3f, 3f});
        datos.setWidthPercentage(100);
        datos.setSpacingBefore(18);
        fila(datos, "Suite", e.getSuite().etiqueta());
        fila(datos, "Tipo de prueba", e.tipo().etiqueta());
        fila(datos, "Alcance", e.getAlcance().etiqueta());
        fila(datos, "Ambiente", e.getAmbiente());
        fila(datos, "Estado de la ejecución", FormatoTexto.estadoEjecucion(e.getEstado())
                + (e.getMensajeError() != null ? " (" + e.getMensajeError() + ")" : ""));
        fila(datos, "Inicio", FormatoTexto.fecha(e.getInicio()));
        fila(datos, "Fin", FormatoTexto.fecha(e.getFin()));
        fila(datos, "Duración", FormatoTexto.duracion(e.duracionMs()));
        fila(datos, "Destinatario del reporte", e.getCorreo().valor());
        fila(datos, "Parámetros", e.getParametros().isEmpty() ? "Valores por defecto" : e.getParametros().toString());
        fila(datos, "Id de la ejecución", e.getUuidPublico().toString());
        fila(datos, "Reporte generado", FormatoTexto.fecha(generadoEn) + " (hora de Bolivia)");
        doc.add(datos);

        Paragraph nota = new Paragraph("Los casos se tipifican por tipo de prueba (funcional o no funcional) y por "
                + "suite. PASS: el caso se cumplió. FAIL: no se cumplió (funcional: PhishShield respondió algo "
                + "distinto de lo esperado; infraestructura: algo no respondió). SKIPPED: se omitió por una "
                + "precondición. NOT_RUN: no se ejecutó, con el motivo indicado.", PEQUENA);
        nota.setSpacingBefore(18);
        doc.add(nota);
    }

    /** Fila de cinco recuadros: total, PASS, FAIL, SKIPPED, NOT_RUN y % de exito. */
    private PdfPTable conteos(ResumenEjecucion r) {
        PdfPTable tabla = new PdfPTable(6);
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(14);
        recuadro(tabla, "Casos", String.valueOf(r.total()), MARINO);
        recuadro(tabla, "PASS", String.valueOf(r.pass()), color(FormatoTexto.colorEstado(EstadoCaso.PASS)));
        recuadro(tabla, "FAIL", String.valueOf(r.fail()), color(FormatoTexto.colorEstado(EstadoCaso.FAIL)));
        recuadro(tabla, "SKIPPED", String.valueOf(r.skipped()), color(FormatoTexto.colorEstado(EstadoCaso.SKIPPED)));
        recuadro(tabla, "NOT_RUN", String.valueOf(r.notRun()), color(FormatoTexto.colorEstado(EstadoCaso.NOT_RUN)));
        recuadro(tabla, "% de éxito", FormatoTexto.numero(r.porcentajeExito()) + " %", PRIMARIO);
        return tabla;
    }

    private void recuadro(PdfPTable tabla, String etiqueta, String valor, Color color) {
        PdfPCell celda = new PdfPCell();
        celda.setBorderColor(BORDE);
        celda.setBorderWidthLeft(3);
        celda.setBorderColorLeft(color);
        celda.setPadding(8);
        celda.addElement(new Paragraph(etiqueta, ETIQUETA));
        celda.addElement(new Paragraph(valor, new Font(Font.HELVETICA, 16, Font.BOLD, color)));
        tabla.addCell(celda);
    }

    // ------------------------------------------------------------------
    // 2. Resumen por tipo y suite
    // ------------------------------------------------------------------

    private void resumen(Document doc, Ejecucion e) {
        titulo(doc, "Resumen por tipo y suite");
        ResumenEjecucion r = e.resumen();
        PdfPTable tabla = tabla(new float[]{1.4f, 1.6f, 0.8f, 0.8f, 0.8f, 1f, 1f, 1f},
                "Tipo", "Suite", "Total", "PASS", "FAIL", "SKIPPED", "NOT_RUN", "% éxito");
        celdas(tabla, e.tipo().etiqueta(), e.getSuite().etiqueta(), String.valueOf(r.total()),
                String.valueOf(r.pass()), String.valueOf(r.fail()), String.valueOf(r.skipped()),
                String.valueOf(r.notRun()), FormatoTexto.numero(r.porcentajeExito()) + " %");
        doc.add(tabla);

        subtitulo(doc, "Por herramienta");
        Map<Herramienta, int[]> porHerramienta = new EnumMap<>(Herramienta.class);
        for (ResultadoCaso c : e.getResultados()) {
            int[] cuenta = porHerramienta.computeIfAbsent(c.getCaso().herramienta(), h -> new int[5]);
            cuenta[0]++;
            cuenta[1 + c.getEstado().ordinal()]++;
        }
        PdfPTable herramientas = tabla(new float[]{2.4f, 0.8f, 0.8f, 0.8f, 1f, 1f},
                "Herramienta", "Total", "PASS", "FAIL", "SKIPPED", "NOT_RUN");
        porHerramienta.forEach((h, c) -> celdas(herramientas, h.etiqueta(), String.valueOf(c[0]),
                String.valueOf(c[1]), String.valueOf(c[2]), String.valueOf(c[3]), String.valueOf(c[4])));
        doc.add(herramientas);

        if (r.fail() > 0) {
            subtitulo(doc, "Fallos por tipo");
            long funcionales = e.getResultados().stream().filter(c -> c.getTipoFallo() == TipoFallo.FUNCIONAL).count();
            PdfPTable fallos = tabla(new float[]{2f, 1f, 4f}, "Tipo de fallo", "Casos", "Qué significa");
            celdas(fallos, "Funcional", String.valueOf(funcionales),
                    "PhishShield respondió, pero no lo esperado: posible defecto del sistema.");
            celdas(fallos, "Infraestructura", String.valueOf(r.fail() - funcionales),
                    "Algo no respondió (servicio caído, sin conexión): no dice si PhishShield funciona.");
            doc.add(fallos);
        }
    }

    // ------------------------------------------------------------------
    // 3. Tabla de casos
    // ------------------------------------------------------------------

    private void tablaCasos(Document doc, Ejecucion e) {
        titulo(doc, "Casos ejecutados");
        PdfPTable tabla = tabla(new float[]{0.4f, 1.6f, 3f, 1.4f, 1.1f, 1.2f, 3.4f},
                "#", "Código", "Caso", "Herramienta", "Estado", "Duración", "Observación");
        for (ResultadoCaso r : e.getResultados()) {
            tabla.addCell(celda(String.valueOf(r.getOrden()), NORMAL));
            tabla.addCell(celda(r.getCodigo(), NEGRITA));
            tabla.addCell(celda(r.getCaso().nombre(), NORMAL));
            tabla.addCell(celda(r.getCaso().herramienta().aValorBd().toUpperCase(), NORMAL));
            tabla.addCell(estado(r.getEstado()));
            tabla.addCell(celda(FormatoTexto.duracion(r.duracionMs()), NORMAL));
            tabla.addCell(celda(r.observacionResumida(), PEQUENA));
        }
        doc.add(tabla);
    }

    // ------------------------------------------------------------------
    // 4. Metricas
    // ------------------------------------------------------------------

    private void metricas(Document doc, Ejecucion e) {
        boolean hay = e.getResultados().stream().anyMatch(r -> !r.getMetricas().isEmpty());
        if (!hay) {
            return;
        }
        titulo(doc, "Métricas de las pruebas no funcionales");
        for (ResultadoCaso r : e.getResultados()) {
            if (!r.getMetricas().isEmpty()) {
                subtitulo(doc, r.getCodigo() + " · " + r.getCaso().nombre());
                doc.add(tablaMetricas(r.getMetricas()));
            }
        }
    }

    private PdfPTable tablaMetricas(List<MetricaRendimiento> metricas) {
        PdfPTable tabla = tabla(new float[]{2.6f, 0.8f, 1.6f, 1.6f, 1.2f}, "Métrica", "Escalón", "Valor", "Umbral", "Evaluación");
        for (MetricaRendimiento m : metricas) {
            tabla.addCell(celda(m.nombre(), NORMAL));
            tabla.addCell(celda(m.escalon() == null ? "-" : String.valueOf(m.escalon()), NORMAL));
            tabla.addCell(celda(FormatoTexto.valorMetrica(m), NEGRITA));
            tabla.addCell(celda(FormatoTexto.umbralMetrica(m), NORMAL));
            Font fuente = m.cumple() == null ? PEQUENA
                    : new Font(Font.HELVETICA, 8, Font.BOLD, m.cumple() ? color("15803d") : color("b91c1c"));
            tabla.addCell(celda(FormatoTexto.cumpleMetrica(m), fuente));
        }
        return tabla;
    }

    // ------------------------------------------------------------------
    // 5. Detalle de cada caso
    // ------------------------------------------------------------------

    private void detalleCaso(Document doc, ResultadoCaso r) {
        PdfPTable cabecera = new PdfPTable(new float[]{5f, 1.2f});
        cabecera.setWidthPercentage(100);
        PdfPCell nombre = new PdfPCell();
        nombre.setBorder(Rectangle.NO_BORDER);
        nombre.setBackgroundColor(SUPERFICIE_2);
        nombre.setPadding(10);
        nombre.addElement(new Paragraph(r.getCodigo(), new Font(Font.HELVETICA, 9, Font.BOLD, PRIMARIO)));
        nombre.addElement(new Paragraph(r.getCaso().nombre(), H1));
        cabecera.addCell(nombre);
        PdfPCell estado = estado(r.getEstado());
        estado.setBorder(Rectangle.NO_BORDER);
        estado.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cabecera.addCell(estado);
        doc.add(cabecera);

        PdfPTable datos = new PdfPTable(new float[]{1.4f, 4f});
        datos.setWidthPercentage(100);
        datos.setSpacingBefore(10);
        fila(datos, "Tipo y suite", r.getCaso().tipo().etiqueta() + " · " + r.getCaso().suite().etiqueta());
        fila(datos, "Herramienta", r.getCaso().herramienta().etiqueta());
        fila(datos, "Objetivo", r.getCaso().objetivo());
        fila(datos, "Precondiciones", r.getCaso().precondiciones());
        fila(datos, "Datos de prueba", r.getCaso().datosPrueba());
        fila(datos, "Resultado esperado", r.getCaso().resultadoEsperado());
        fila(datos, "Resultado obtenido", r.observacionResumida());
        if (r.getTipoFallo() != null) {
            fila(datos, "Tipo de fallo", r.getTipoFallo().etiqueta());
            fila(datos, "Paso fallido", FormatoTexto.vacioComoGuion(r.getPasoFallido()));
            fila(datos, "Mensaje de error", FormatoTexto.vacioComoGuion(r.getMensajeError()));
        }
        fila(datos, "Inicio / fin", FormatoTexto.fecha(r.getInicio()) + "  /  " + FormatoTexto.fecha(r.getFin()));
        fila(datos, "Duración", FormatoTexto.duracion(r.duracionMs()));
        doc.add(datos);

        if (!r.getPasos().isEmpty()) {
            subtitulo(doc, "Pasos ejecutados");
            PdfPTable pasos = tabla(new float[]{0.4f, 3f, 3.6f, 0.8f}, "N.º", "Paso", "Resultado observado", "Estado");
            for (PasoEjecutado p : r.getPasos()) {
                pasos.addCell(celda(String.valueOf(p.numero()), NORMAL));
                pasos.addCell(celda(p.descripcion(), NORMAL));
                pasos.addCell(celda(FormatoTexto.vacioComoGuion(p.resultadoObservado()), PEQUENA));
                pasos.addCell(celda(p.fallo() ? "FALLO" : "OK",
                        new Font(Font.HELVETICA, 8, Font.BOLD, p.fallo() ? color("b91c1c") : color("15803d"))));
            }
            doc.add(pasos);
            evidencias(doc, r.getPasos());
        }

        if (!r.getMetricas().isEmpty()) {
            subtitulo(doc, "Métricas");
            doc.add(tablaMetricas(r.getMetricas()));
        }
    }

    private void evidencias(Document doc, List<PasoEjecutado> pasos) {
        boolean titulo = false;
        for (PasoEjecutado p : pasos) {
            if (p.detalleApi() == null && !p.tieneCaptura()) {
                continue;
            }
            if (!titulo) {
                subtitulo(doc, "Evidencias");
                titulo = true;
            }
            Paragraph cabecera = new Paragraph("Paso " + p.numero() + ": " + p.descripcion(), ETIQUETA);
            cabecera.setSpacingBefore(6);
            doc.add(cabecera);

            if (p.detalleApi() != null) {
                PdfPTable caja = new PdfPTable(1);
                caja.setWidthPercentage(100);
                caja.setSpacingBefore(3);
                String texto = p.detalleApi().length() > MAXIMO_DETALLE_API
                        ? p.detalleApi().substring(0, MAXIMO_DETALLE_API) + "\n… (recortado; detalle completo en la pantalla)"
                        : p.detalleApi();
                PdfPCell celda = new PdfPCell(new Phrase(texto, MONO));
                celda.setBackgroundColor(SUPERFICIE_2);
                celda.setBorderColor(BORDE);
                celda.setPadding(6);
                caja.addCell(celda);
                doc.add(caja);
            }
            if (p.tieneCaptura()) {
                doc.add(captura(p.rutaCaptura()));
            }
        }
    }

    /** La imagen de la captura; si no esta en disco, un aviso en su lugar. */
    private Element captura(String ruta) {
        Optional<Path> archivo = carpeta.resolver(ruta).filter(Files::isRegularFile);
        if (archivo.isPresent()) {
            try {
                Image imagen = Image.getInstance(Files.readAllBytes(archivo.get()));
                imagen.scaleToFit(515, 330);
                imagen.setSpacingBefore(4);
                imagen.setBorder(Rectangle.BOX);
                imagen.setBorderColor(BORDE);
                imagen.setBorderWidth(1);
                return imagen;
            } catch (Exception ex) {
                return aviso("Captura no disponible: el archivo " + ruta + " no se pudo leer (" + ex.getMessage() + ")");
            }
        }
        return aviso("Captura no disponible: no se encontró el archivo " + ruta);
    }

    private Paragraph aviso(String texto) {
        Paragraph p = new Paragraph(texto, new Font(Font.HELVETICA, 8, Font.ITALIC, color("a14a06")));
        p.setSpacingBefore(3);
        return p;
    }

    // ------------------------------------------------------------------
    // Ayudas de maquetacion
    // ------------------------------------------------------------------

    private static void titulo(Document doc, String texto) {
        Paragraph p = new Paragraph(texto, H1);
        p.setSpacingBefore(14);
        p.setSpacingAfter(8);
        doc.add(p);
    }

    private static void subtitulo(Document doc, String texto) {
        Paragraph p = new Paragraph(texto, H2);
        p.setSpacingBefore(12);
        p.setSpacingAfter(5);
        doc.add(p);
    }

    private static PdfPTable tabla(float[] anchos, String... encabezados) {
        PdfPTable tabla = new PdfPTable(anchos);
        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(1);
        for (String encabezado : encabezados) {
            PdfPCell celda = new PdfPCell(new Phrase(encabezado, ENCABEZADO_TABLA));
            celda.setBackgroundColor(MARINO);
            celda.setBorderColor(MARINO);
            celda.setPadding(5);
            tabla.addCell(celda);
        }
        return tabla;
    }

    private static void celdas(PdfPTable tabla, String... textos) {
        for (String texto : textos) {
            tabla.addCell(celda(texto, NORMAL));
        }
    }

    private static PdfPCell celda(String texto, Font fuente) {
        PdfPCell celda = new PdfPCell(new Phrase(texto == null ? "-" : texto, fuente));
        celda.setBorderColor(BORDE);
        celda.setPadding(5);
        return celda;
    }

    private static void fila(PdfPTable tabla, String etiqueta, String valor) {
        PdfPCell izquierda = celda(etiqueta, ETIQUETA);
        izquierda.setBackgroundColor(SUPERFICIE_2);
        tabla.addCell(izquierda);
        tabla.addCell(celda(FormatoTexto.vacioComoGuion(valor), NORMAL));
    }

    private static PdfPCell estado(EstadoCaso estado) {
        PdfPCell celda = new PdfPCell(new Phrase(FormatoTexto.estadoCaso(estado), ENCABEZADO_TABLA));
        celda.setBackgroundColor(color(FormatoTexto.colorEstado(estado)));
        celda.setBorderColor(BORDE);
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        celda.setPadding(5);
        return celda;
    }

    private static Color color(String hex) {
        return new Color(Integer.parseInt(hex, 16));
    }

    /** Pie de cada pagina: que reporte es y el numero de pagina. */
    private static final class PiePagina extends PdfPageEventHelper {

        private final String texto;

        private PiePagina(Ejecucion e) {
            this.texto = "PhishShield Bolivia · Reporte de automatización · " + e.getSuite().etiqueta()
                    + " · " + FormatoTexto.fecha(e.getInicio() != null ? e.getInicio() : e.getCreadaEn());
        }

        @Override
        public void onEndPage(PdfWriter escritor, Document doc) {
            ColumnText.showTextAligned(escritor.getDirectContent(), Element.ALIGN_LEFT,
                    new Phrase(texto, PEQUENA), doc.left(), doc.bottom() - 22, 0);
            ColumnText.showTextAligned(escritor.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase("Página " + escritor.getPageNumber(), PEQUENA), doc.right(), doc.bottom() - 22, 0);
        }
    }
}
