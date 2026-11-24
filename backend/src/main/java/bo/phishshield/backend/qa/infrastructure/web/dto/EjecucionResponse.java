package bo.phishshield.backend.qa.infrastructure.web.dto;

import bo.phishshield.backend.qa.domain.model.Ejecucion;
import bo.phishshield.backend.qa.domain.model.MetricaRendimiento;
import bo.phishshield.backend.qa.domain.model.PasoEjecutado;
import bo.phishshield.backend.qa.domain.model.ResultadoCaso;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static bo.phishshield.backend.shared.infrastructure.web.dto.FechasLocales.enZonaLocal;

/**
 * Detalle de una ejecucion con sus casos, pasos y metricas. Los estados van
 * en minusculas, igual que en la base (pass, fail, en_curso...).
 */
public record EjecucionResponse(
        UUID id,
        String tipo,
        String suite,
        String suiteEtiqueta,
        String alcance,
        String correo,
        String ambiente,
        Map<String, String> parametros,
        String estado,
        boolean activa,
        String resultadoGlobal,
        OffsetDateTime creadaEn,
        OffsetDateTime inicio,
        OffsetDateTime fin,
        Long duracionMs,
        String mensajeError,
        String estadoCorreo,
        String errorCorreo,
        OffsetDateTime correoEnviadoEn,
        boolean tieneReportes,
        ResumenResponse resumen,
        List<Resultado> resultados
) {

    public record Resultado(
            String codigo,
            String nombre,
            String herramienta,
            int orden,
            String estado,
            String tipoFallo,
            String mensajeError,
            String pasoFallido,
            String observacion,
            String observacionResumida,
            OffsetDateTime inicio,
            OffsetDateTime fin,
            Long duracionMs,
            List<Paso> pasos,
            List<Metrica> metricas
    ) {
        static Resultado desde(ResultadoCaso r) {
            return new Resultado(
                    r.getCodigo(), r.getCaso().nombre(), r.getCaso().herramienta().aValorBd(), r.getOrden(),
                    r.getEstado().aValorBd(),
                    r.getTipoFallo() == null ? null : r.getTipoFallo().aValorBd(),
                    r.getMensajeError(), r.getPasoFallido(), r.getObservacion(), r.observacionResumida(),
                    enZonaLocal(r.getInicio()), enZonaLocal(r.getFin()), r.duracionMs(),
                    r.getPasos().stream().map(Paso::desde).toList(),
                    r.getMetricas().stream().map(Metrica::desde).toList());
        }
    }

    /** rutaCaptura es relativa a la carpeta de la ejecucion; la imagen se pide aparte. */
    public record Paso(int numero, String descripcion, String resultadoObservado, String estado,
                       String rutaCaptura, String detalleApi, OffsetDateTime registradoEn) {
        static Paso desde(PasoEjecutado p) {
            return new Paso(p.numero(), p.descripcion(), p.resultadoObservado(), p.estado().aValorBd(),
                    p.rutaCaptura(), p.detalleApi(), enZonaLocal(p.registradoEn()));
        }
    }

    public record Metrica(String nombre, BigDecimal valor, String unidad, BigDecimal umbral,
                          Boolean cumple, Integer escalon) {
        static Metrica desde(MetricaRendimiento m) {
            return new Metrica(m.nombre(), m.valor(), m.unidad(), m.umbral(), m.cumple(), m.escalon());
        }
    }

    public static EjecucionResponse desde(Ejecucion e) {
        return new EjecucionResponse(
                e.getUuidPublico(),
                e.tipo().aValorBd(),
                e.getSuite().aValorBd(), e.getSuite().etiqueta(),
                e.getAlcance().aValorBd(),
                e.getCorreo().valor(),
                e.getAmbiente(),
                e.getParametros(),
                e.getEstado().aValorBd(),
                e.getEstado().estaActiva(),
                e.getResultadoGlobal() == null ? null : e.getResultadoGlobal().aValorBd(),
                enZonaLocal(e.getCreadaEn()), enZonaLocal(e.getInicio()), enZonaLocal(e.getFin()),
                e.duracionMs(),
                e.getMensajeError(),
                e.getEstadoCorreo().aValorBd(),
                e.getErrorCorreo(),
                enZonaLocal(e.getCorreoEnviadoEn()),
                e.tieneReportes(),
                ResumenResponse.desde(e.resumen()),
                e.getResultados().stream().map(Resultado::desde).toList());
    }
}
