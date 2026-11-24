package bo.phishshield.backend.qa.infrastructure.reporte;

import bo.phishshield.backend.qa.domain.model.Ejecucion;
import bo.phishshield.backend.qa.domain.model.EstadoCaso;
import bo.phishshield.backend.qa.domain.model.MetricaRendimiento;
import bo.phishshield.backend.qa.domain.model.ResultadoCaso;
import bo.phishshield.backend.qa.domain.model.ResumenEjecucion;

import java.time.OffsetDateTime;

/**
 * Reporte HTML RESUMIDO: un solo archivo, sin recursos externos (CSS en
 * linea, sin scripts ni fuentes de internet), para abrirlo con doble clic
 * o adjuntarlo a un correo. Muestra el resultado global, los conteos, la
 * tabla de casos con su observacion y las metricas.
 */
final class ReporteHtml {

    private static final String CSS = """
            :root{--marino:#0f1f3d;--primario:#1d4ed8;--texto:#0f172a;--texto-2:#475569;--borde:#e2e8f0;
            --fondo:#f6f8fb;--superficie:#fff;--superficie-2:#f1f5f9;--pass:#15803d;--fail:#b91c1c;
            --skipped:#a14a06;--notrun:#64748b}
            *{box-sizing:border-box}
            body{margin:0;background:var(--fondo);color:var(--texto);font:14px/1.5 "Segoe UI",system-ui,-apple-system,Roboto,sans-serif}
            .contenedor{max-width:1080px;margin:0 auto;padding:24px 16px 40px}
            header{background:var(--marino);color:#fff;border-radius:14px;padding:24px 28px}
            header .marca{color:#bfdbfe;font-size:13px;margin:0}
            header h1{margin:6px 0 4px;font-size:24px}
            header p{margin:0;color:#dbeafe}
            .resultado{display:inline-block;margin-top:14px;padding:4px 12px;border-radius:999px;font-weight:700;background:#fff}
            .tarjetas{display:grid;grid-template-columns:repeat(auto-fit,minmax(130px,1fr));gap:12px;margin:18px 0}
            .tarjeta{background:var(--superficie);border:1px solid var(--borde);border-left-width:4px;border-radius:10px;padding:10px 14px}
            .tarjeta span{display:block;font-size:12px;color:var(--texto-2);font-weight:600}
            .tarjeta strong{font-size:24px}
            section{background:var(--superficie);border:1px solid var(--borde);border-radius:12px;padding:18px 20px;margin-top:16px}
            h2{margin:0 0 12px;font-size:17px;color:var(--marino)}
            dl{display:grid;grid-template-columns:max-content 1fr;gap:6px 18px;margin:0}
            dt{color:var(--texto-2);font-weight:600}dd{margin:0}
            .tabla{overflow-x:auto}
            table{width:100%;border-collapse:collapse;font-size:13px}
            th{background:var(--superficie-2);color:var(--texto-2);text-align:left;font-weight:600;padding:8px 10px;border-bottom:1px solid var(--borde)}
            td{padding:8px 10px;border-bottom:1px solid var(--borde);vertical-align:top}
            .codigo{font-weight:700;white-space:nowrap}
            .estado{display:inline-block;min-width:74px;text-align:center;color:#fff;font-weight:700;font-size:12px;border-radius:999px;padding:2px 8px}
            .PASS{background:var(--pass)}.FAIL{background:var(--fail)}.SKIPPED{background:var(--skipped)}.NOT_RUN{background:var(--notrun)}
            .obs{color:var(--texto-2)}
            .cumple{color:var(--pass);font-weight:700}.no-cumple{color:var(--fail);font-weight:700}
            footer{margin-top:18px;color:var(--texto-2);font-size:12px}
            @media (max-width:600px){header{padding:18px}dl{grid-template-columns:1fr}}
            @media print{body{background:#fff}section,header{break-inside:avoid}}
            """;

    private ReporteHtml() {
    }

    static String generar(Ejecucion e, OffsetDateTime generadoEn) {
        ResumenEjecucion r = e.resumen();
        String colorResultado = "#" + FormatoTexto.colorResultado(e.getResultadoGlobal());
        StringBuilder h = new StringBuilder(16_000);

        h.append("<!doctype html>\n<html lang=\"es\">\n<head>\n<meta charset=\"utf-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
                .append("<title>Reporte ").append(esc(e.getSuite().etiqueta())).append(" - PhishShield QA</title>\n")
                .append("<style>").append(CSS).append("</style>\n</head>\n<body>\n<div class=\"contenedor\">\n");

        h.append("<header>\n<p class=\"marca\">PhishShield Bolivia · Centro de Pruebas QA</p>\n")
                .append("<h1>Reporte de automatización · ").append(esc(e.getSuite().etiqueta())).append("</h1>\n")
                .append("<p>").append(esc(e.tipo().etiqueta())).append(" · ").append(esc(e.getAlcance().etiqueta()))
                .append(" · ambiente ").append(esc(e.getAmbiente())).append("</p>\n")
                .append("<span class=\"resultado\" style=\"color:").append(colorResultado).append("\">Resultado global: ")
                .append(esc(FormatoTexto.resultadoGlobal(e.getResultadoGlobal(), e.getEstado()))).append("</span>\n")
                .append("</header>\n");

        h.append("<div class=\"tarjetas\">\n");
        tarjeta(h, "Casos", String.valueOf(r.total()), "#0f1f3d");
        tarjeta(h, "PASS", String.valueOf(r.pass()), "#" + FormatoTexto.colorEstado(EstadoCaso.PASS));
        tarjeta(h, "FAIL", String.valueOf(r.fail()), "#" + FormatoTexto.colorEstado(EstadoCaso.FAIL));
        tarjeta(h, "SKIPPED", String.valueOf(r.skipped()), "#" + FormatoTexto.colorEstado(EstadoCaso.SKIPPED));
        tarjeta(h, "NOT_RUN", String.valueOf(r.notRun()), "#" + FormatoTexto.colorEstado(EstadoCaso.NOT_RUN));
        tarjeta(h, "% de éxito", FormatoTexto.numero(r.porcentajeExito()) + " %", "#1d4ed8");
        h.append("</div>\n");

        h.append("<section>\n<h2>Datos de la ejecución</h2>\n<dl>\n");
        dato(h, "Estado", FormatoTexto.estadoEjecucion(e.getEstado())
                + (e.getMensajeError() != null ? " (" + e.getMensajeError() + ")" : ""));
        dato(h, "Inicio", FormatoTexto.fecha(e.getInicio()));
        dato(h, "Fin", FormatoTexto.fecha(e.getFin()));
        dato(h, "Duración", FormatoTexto.duracion(e.duracionMs()));
        dato(h, "Destinatario", e.getCorreo().valor());
        dato(h, "Id", e.getUuidPublico().toString());
        h.append("</dl>\n</section>\n");

        h.append("<section>\n<h2>Casos</h2>\n<div class=\"tabla\"><table>\n<thead><tr>")
                .append("<th>Código</th><th>Caso</th><th>Herramienta</th><th>Estado</th><th>Duración</th><th>Observación</th>")
                .append("</tr></thead>\n<tbody>\n");
        for (ResultadoCaso c : e.getResultados()) {
            h.append("<tr><td class=\"codigo\">").append(esc(c.getCodigo())).append("</td><td>")
                    .append(esc(c.getCaso().nombre())).append("</td><td>")
                    .append(esc(c.getCaso().herramienta().aValorBd().toUpperCase())).append("</td><td>")
                    .append("<span class=\"estado ").append(c.getEstado().name()).append("\">")
                    .append(c.getEstado().name()).append("</span></td><td>")
                    .append(esc(FormatoTexto.duracion(c.duracionMs()))).append("</td><td class=\"obs\">")
                    .append(esc(c.observacionResumida())).append("</td></tr>\n");
        }
        h.append("</tbody></table></div>\n</section>\n");

        boolean hayMetricas = e.getResultados().stream().anyMatch(c -> !c.getMetricas().isEmpty());
        if (hayMetricas) {
            h.append("<section>\n<h2>Métricas</h2>\n<div class=\"tabla\"><table>\n<thead><tr>")
                    .append("<th>Caso</th><th>Métrica</th><th>Escalón</th><th>Valor</th><th>Umbral</th><th>Evaluación</th>")
                    .append("</tr></thead>\n<tbody>\n");
            for (ResultadoCaso c : e.getResultados()) {
                for (MetricaRendimiento m : c.getMetricas()) {
                    String clase = m.cumple() == null ? "obs" : (m.cumple() ? "cumple" : "no-cumple");
                    h.append("<tr><td class=\"codigo\">").append(esc(c.getCodigo())).append("</td><td>")
                            .append(esc(m.nombre())).append("</td><td>")
                            .append(m.escalon() == null ? "-" : m.escalon()).append("</td><td>")
                            .append(esc(FormatoTexto.valorMetrica(m))).append("</td><td>")
                            .append(esc(FormatoTexto.umbralMetrica(m))).append("</td><td class=\"").append(clase).append("\">")
                            .append(esc(FormatoTexto.cumpleMetrica(m))).append("</td></tr>\n");
                }
            }
            h.append("</tbody></table></div>\n</section>\n");
        }

        h.append("<footer>Generado el ").append(esc(FormatoTexto.fecha(generadoEn)))
                .append(" (hora de Bolivia). Pasos, evidencias y capturas de cada caso: reporte detallado en PDF.")
                .append("</footer>\n</div>\n</body>\n</html>\n");
        return h.toString();
    }

    private static void tarjeta(StringBuilder h, String etiqueta, String valor, String color) {
        h.append("<div class=\"tarjeta\" style=\"border-left-color:").append(color).append("\"><span>")
                .append(esc(etiqueta)).append("</span><strong style=\"color:").append(color).append("\">")
                .append(esc(valor)).append("</strong></div>\n");
    }

    private static void dato(StringBuilder h, String etiqueta, String valor) {
        h.append("<dt>").append(esc(etiqueta)).append("</dt><dd>").append(esc(valor)).append("</dd>\n");
    }

    /** Escapa el texto para HTML: un mensaje de error con "<script>" se muestra, no se ejecuta. */
    static String esc(String texto) {
        if (texto == null) {
            return "";
        }
        StringBuilder s = new StringBuilder(texto.length());
        for (char c : texto.toCharArray()) {
            switch (c) {
                case '<' -> s.append("&lt;");
                case '>' -> s.append("&gt;");
                case '&' -> s.append("&amp;");
                case '"' -> s.append("&quot;");
                case '\'' -> s.append("&#39;");
                default -> s.append(c);
            }
        }
        return s.toString();
    }
}
