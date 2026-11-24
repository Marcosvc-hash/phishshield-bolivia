package bo.phishshield.backend.reporte.infrastructure.pdf;

import bo.phishshield.backend.reporte.domain.model.ReporteEmpresa;
import bo.phishshield.backend.reporte.domain.model.TotalesReporte;
import bo.phishshield.backend.reporte.domain.port.out.ReportePdfPort;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
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
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * PDF de un reporte de empresa (HU-17), con OpenPDF y la paleta del
 * frontend: encabezado, datos del reporte, tres totales y su lectura.
 */
public class ReporteEmpresaPdfAdapter implements ReportePdfPort {

    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Color MARINO = new Color(0x0f1f3d);
    private static final Color PRIMARIO = new Color(0x1d4ed8);
    private static final Color PELIGRO = new Color(0xb91c1c);
    private static final Color ALERTA = new Color(0xa14a06);
    private static final Color TEXTO_2 = new Color(0x475569);
    private static final Color BORDE = new Color(0xe2e8f0);
    private static final Color SUPERFICIE_2 = new Color(0xf1f5f9);

    private static final Font TITULO = new Font(Font.HELVETICA, 22, Font.BOLD, Color.WHITE);
    private static final Font SUBTITULO = new Font(Font.HELVETICA, 11, Font.NORMAL, new Color(0xbfdbfe));
    private static final Font H2 = new Font(Font.HELVETICA, 13, Font.BOLD, MARINO);
    private static final Font NORMAL = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(0x0f172a));
    private static final Font ETIQUETA = new Font(Font.HELVETICA, 9, Font.BOLD, TEXTO_2);
    private static final Font PEQUENA = new Font(Font.HELVETICA, 8, Font.NORMAL, TEXTO_2);

    @Override
    public byte[] generar(ReporteEmpresa r) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 48, 48, 48, 56);
        PdfWriter escritor = PdfWriter.getInstance(doc, salida);
        escritor.setPageEvent(new Pie(r));
        doc.addTitle("Reporte de " + r.empresa().nombre());
        doc.addAuthor("PhishShield Bolivia");
        doc.open();

        PdfPTable banda = new PdfPTable(1);
        banda.setWidthPercentage(100);
        PdfPCell celda = new PdfPCell();
        celda.setBackgroundColor(MARINO);
        celda.setBorder(Rectangle.NO_BORDER);
        celda.setPadding(24);
        celda.addElement(new Paragraph("PhishShield Bolivia · Reporte por empresa", SUBTITULO));
        Paragraph titulo = new Paragraph(r.empresa().nombre(), TITULO);
        titulo.setSpacingBefore(6);
        celda.addElement(titulo);
        Paragraph periodo = new Paragraph("Período: " + DIA.format(r.periodo().desde()) + " al "
                + DIA.format(r.periodo().hasta()) + " (" + r.periodo().dias() + " días)", SUBTITULO);
        periodo.setSpacingBefore(4);
        celda.addElement(periodo);
        banda.addCell(celda);
        doc.add(banda);

        TotalesReporte t = r.totales();
        PdfPTable totales = new PdfPTable(3);
        totales.setWidthPercentage(100);
        totales.setSpacingBefore(20);
        recuadro(totales, "Análisis realizados", String.valueOf(t.analisis()), PRIMARIO);
        recuadro(totales, "Phishing detectado", t.phishing() + "  (" + porcentaje(t) + " %)", PELIGRO);
        recuadro(totales, "Alertas generadas", String.valueOf(t.alertas()), ALERTA);
        doc.add(totales);

        Paragraph lectura = new Paragraph(lectura(r), NORMAL);
        lectura.setSpacingBefore(16);
        doc.add(lectura);

        Paragraph datos = new Paragraph("Datos del reporte", H2);
        datos.setSpacingBefore(20);
        datos.setSpacingAfter(6);
        doc.add(datos);
        PdfPTable tabla = new PdfPTable(new float[]{1.3f, 3f});
        tabla.setWidthPercentage(100);
        fila(tabla, "Empresa", r.empresa().nombre() + (r.empresa().activa() ? "" : " (desactivada)"));
        fila(tabla, "Período", DIA.format(r.periodo().desde()) + " al " + DIA.format(r.periodo().hasta()));
        fila(tabla, "Generado por", r.generadoPor() == null ? "-" : r.generadoPor());
        fila(tabla, "Fecha de generación", fecha(r.fechaGeneracion()) + " (hora de Bolivia)");
        fila(tabla, "Identificador", r.uuidPublico().toString());
        doc.add(tabla);

        Paragraph nota = new Paragraph("Los totales se calcularon desde la base de PhishShield al generar el reporte: "
                + "análisis de la empresa con fecha dentro del período (hora de Bolivia), cuántos fueron phishing y "
                + "cuántas alertas abrieron esos análisis (las alertas se abren para resultados sospechosos y de phishing).",
                PEQUENA);
        nota.setSpacingBefore(16);
        doc.add(nota);

        doc.close();
        return salida.toByteArray();
    }

    static String lectura(ReporteEmpresa r) {
        TotalesReporte t = r.totales();
        if (t.analisis() == 0) {
            return "En este período no se registraron análisis a nombre de " + r.empresa().nombre() + ".";
        }
        return "En el período se analizaron " + t.analisis() + " enlace(s) a nombre de " + r.empresa().nombre()
                + "; " + t.phishing() + " fueron phishing (" + porcentaje(t) + " %) y se abrieron " + t.alertas()
                + " alerta(s) para revisar.";
    }

    private static String porcentaje(TotalesReporte t) {
        return t.porcentajePhishing().toPlainString().replace('.', ',');
    }

    private static String fecha(OffsetDateTime f) {
        return f == null ? "-" : f.atZoneSameInstant(ZONA).format(FECHA_HORA);
    }

    private static void recuadro(PdfPTable tabla, String etiqueta, String valor, Color color) {
        PdfPCell celda = new PdfPCell();
        celda.setBorderColor(BORDE);
        celda.setBorderWidthLeft(3);
        celda.setBorderColorLeft(color);
        celda.setPadding(10);
        celda.addElement(new Paragraph(etiqueta, ETIQUETA));
        celda.addElement(new Paragraph(valor, new Font(Font.HELVETICA, 18, Font.BOLD, color)));
        tabla.addCell(celda);
    }

    private static void fila(PdfPTable tabla, String etiqueta, String valor) {
        PdfPCell izquierda = new PdfPCell(new Phrase(etiqueta, ETIQUETA));
        izquierda.setBackgroundColor(SUPERFICIE_2);
        izquierda.setBorderColor(BORDE);
        izquierda.setPadding(6);
        tabla.addCell(izquierda);
        PdfPCell derecha = new PdfPCell(new Phrase(valor, NORMAL));
        derecha.setBorderColor(BORDE);
        derecha.setPadding(6);
        tabla.addCell(derecha);
    }

    private static final class Pie extends PdfPageEventHelper {
        private final String texto;

        private Pie(ReporteEmpresa r) {
            texto = "PhishShield Bolivia · Reporte de " + r.empresa().nombre() + " · generado el " + fecha(r.fechaGeneracion());
        }

        @Override
        public void onEndPage(PdfWriter escritor, Document doc) {
            ColumnText.showTextAligned(escritor.getDirectContent(), Element.ALIGN_LEFT, new Phrase(texto, PEQUENA),
                    doc.left(), doc.bottom() - 24, 0);
            ColumnText.showTextAligned(escritor.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase("Página " + escritor.getPageNumber(), PEQUENA), doc.right(), doc.bottom() - 24, 0);
        }
    }
}
