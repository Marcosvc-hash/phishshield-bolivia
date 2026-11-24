package bo.phishshield.backend.qa.infrastructure.ejecutor.casos.carga;

import bo.phishshield.backend.qa.domain.model.MatrizConfusion;
import bo.phishshield.backend.qa.domain.model.MetricaRendimiento;
import bo.phishshield.backend.qa.domain.model.UrlEtiquetada;
import bo.phishshield.backend.qa.infrastructure.ejecutor.CasoQA;
import bo.phishshield.backend.qa.infrastructure.ejecutor.ContextoCaso;
import bo.phishshield.backend.qa.infrastructure.ejecutor.ExtensionQA;
import bo.phishshield.backend.qa.infrastructure.ejecutor.FalloInfraestructuraException;
import bo.phishshield.backend.qa.infrastructure.ejecutor.carga.ClienteJson;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Suite Robustez del modelo IA (no funcional): precision, recall, F1 y
 * matriz de confusion sobre el lote boliviano etiquetado (qa_lote_robustez).
 *
 * TC-ROB-001 mide el sistema completo (reglas + IA) por la API del backend;
 * TC-ROB-002 mide solo el modelo, preguntando directo al servicio Python.
 * Umbrales por defecto 0,80 (parametros umbralPrecision y umbralRecall).
 */
@ExtendWith(ExtensionQA.class)
public class CasosRobustez {

    /** Lo que dijo el detector para una URL: detectada o no, o null si no hubo respuesta. */
    private record Veredicto(UrlEtiquetada url, Boolean detectada, String detalle) {
    }

    @Test
    @CasoQA("TC-ROB-001")
    void precisionYRecallDelSistemaCompleto(ContextoCaso ctx) {
        ctx.requiereBackend();
        String api = ctx.entorno().baseUrl() + "/api/analisis";
        medir(ctx, "POST /api/analisis (reglas + IA); detectada = sospechoso o phishing", url -> {
            ClienteJson.Respuesta r = new ClienteJson().post(api, Map.of("url", url.url(), "origen", "api"));
            if (!r.ok()) {
                return new Veredicto(url, null, r.estado() == 0 ? r.error() : "HTTP " + r.estado());
            }
            String resultado = String.valueOf(r.cuerpo().get("resultado"));
            return new Veredicto(url, !"seguro".equals(resultado), resultado + " " + r.cuerpo().get("nivelRiesgo"));
        });
    }

    @Test
    @CasoQA("TC-ROB-002")
    void precisionYRecallDelModeloSolo(ContextoCaso ctx) {
        ctx.requiereServicioIa();
        String predecir = ctx.entorno().iaUrl() + "/predecir";
        medir(ctx, "POST " + predecir + " (solo el modelo); detectada = es_phishing", url -> {
            ClienteJson.Respuesta r = new ClienteJson().post(predecir, Map.of("url", url.url()));
            if (!r.ok()) {
                return new Veredicto(url, null, r.estado() == 0 ? r.error() : "HTTP " + r.estado());
            }
            return new Veredicto(url, Boolean.TRUE.equals(r.cuerpo().get("es_phishing")),
                    "p=" + r.cuerpo().get("probabilidad_phishing"));
        });
    }

    private static void medir(ContextoCaso ctx, String como, Function<UrlEtiquetada, Veredicto> detector) {
        double umbralPrecision = ctx.parametroDecimal("umbralPrecision", 0.80);
        double umbralRecall = ctx.parametroDecimal("umbralRecall", 0.80);

        List<UrlEtiquetada> lote = ctx.paso("Leer el lote activo y aprobado de qa_lote_robustez", () -> {
            List<UrlEtiquetada> urls = ctx.loteRobustez();
            long phishing = urls.stream().filter(UrlEtiquetada::esPhishing).count();
            ctx.observar(urls.size() + " URL: " + phishing + " de phishing y " + (urls.size() - phishing) + " legítimas");
            return urls;
        });
        if (lote.isEmpty()) {
            Assumptions.abort("El lote de robustez está vacío (no hay URL activas y aprobadas)");
        }

        List<Veredicto> veredictos = ctx.paso("Analizar las " + lote.size() + " URL con " + como, () -> {
            List<Veredicto> lista = new ArrayList<>();
            long inicio = System.nanoTime();
            for (UrlEtiquetada url : lote) {
                if (ctx.detenida()) {
                    Assumptions.abort("Se detuvo la ejecución mientras se recorría el lote");
                }
                lista.add(detector.apply(url));
            }
            List<Veredicto> sinRespuesta = lista.stream().filter(v -> v.detectada() == null).toList();
            ctx.observar(lista.size() + " URL en " + (System.nanoTime() - inicio) / 1_000_000 + " ms; sin respuesta: "
                    + sinRespuesta.size() + (sinRespuesta.isEmpty() ? "" : " (por ejemplo " + sinRespuesta.getFirst().detalle() + ")"));
            if (!sinRespuesta.isEmpty()) {
                throw new FalloInfraestructuraException(sinRespuesta.size() + " URL del lote no se pudieron analizar: "
                        + sinRespuesta.getFirst().detalle());
            }
            return lista;
        });

        MatrizConfusion matriz = ctx.paso("Calcular la matriz de confusión", () -> {
            MatrizConfusion m = MatrizConfusion.vacia();
            Map<String, int[]> porCategoria = new TreeMap<>();
            List<String> noDetectadas = new ArrayList<>();
            List<String> falsasAlarmas = new ArrayList<>();
            for (Veredicto v : veredictos) {
                m = m.con(v.url().esPhishing(), v.detectada());
                int[] cuenta = porCategoria.computeIfAbsent(v.url().categoria(), c -> new int[2]);
                cuenta[0]++;
                if (v.detectada() == v.url().esPhishing()) {
                    cuenta[1]++;
                } else if (v.url().esPhishing()) {
                    noDetectadas.add(v.url().url() + " (" + v.detalle() + ")");
                } else {
                    falsasAlarmas.add(v.url().url() + " (" + v.detalle() + ")");
                }
            }
            StringBuilder texto = new StringBuilder("VP=" + m.vp() + ", FP=" + m.fp() + ", VN=" + m.vn() + ", FN=" + m.fn()
                    + ". Aciertos por categoría: ");
            porCategoria.forEach((c, n) -> texto.append(c).append(' ').append(n[1]).append('/').append(n[0]).append("; "));
            texto.append("Phishing no detectado: ").append(noDetectadas.isEmpty() ? "ninguno" : noDetectadas)
                    .append(". Legítimas marcadas como phishing: ").append(falsasAlarmas.isEmpty() ? "ninguna" : falsasAlarmas);
            ctx.observar(texto.toString());
            return m;
        });

        ctx.metrica(MetricaRendimiento.conMinimo("Precisión", matriz.precision(), "proporción", BigDecimal.valueOf(umbralPrecision)));
        ctx.metrica(MetricaRendimiento.conMinimo("Recall", matriz.recall(), "proporción", BigDecimal.valueOf(umbralRecall)));
        ctx.metrica(MetricaRendimiento.informativa("F1", matriz.f1(), "proporción"));
        ctx.metrica(MetricaRendimiento.informativa("Exactitud", matriz.exactitud(), "proporción"));
        ctx.metrica(MetricaRendimiento.informativa("Verdaderos positivos (VP)", BigDecimal.valueOf(matriz.vp()), "URL"));
        ctx.metrica(MetricaRendimiento.informativa("Falsos positivos (FP)", BigDecimal.valueOf(matriz.fp()), "URL"));
        ctx.metrica(MetricaRendimiento.informativa("Verdaderos negativos (VN)", BigDecimal.valueOf(matriz.vn()), "URL"));
        ctx.metrica(MetricaRendimiento.informativa("Falsos negativos (FN)", BigDecimal.valueOf(matriz.fn()), "URL"));

        ctx.paso("Verificar precisión >= " + umbralPrecision + " y recall >= " + umbralRecall, () -> {
            ctx.observar("Precisión " + matriz.precision() + ", recall " + matriz.recall() + ", F1 " + matriz.f1()
                    + ", exactitud " + matriz.exactitud());
            assertTrue(matriz.precision().doubleValue() >= umbralPrecision,
                    "Precisión " + matriz.precision() + " (umbral " + umbralPrecision + ")");
            assertTrue(matriz.recall().doubleValue() >= umbralRecall,
                    "Recall " + matriz.recall() + " (umbral " + umbralRecall + ")");
        });
    }
}
