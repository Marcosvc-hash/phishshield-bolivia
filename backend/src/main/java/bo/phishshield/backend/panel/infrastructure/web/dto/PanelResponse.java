package bo.phishshield.backend.panel.infrastructure.web.dto;

import bo.phishshield.backend.panel.domain.model.AnalisisDia;
import bo.phishshield.backend.panel.domain.model.AnalisisReciente;
import bo.phishshield.backend.panel.domain.model.EntidadTop;
import bo.phishshield.backend.panel.domain.model.Panel;
import bo.phishshield.backend.panel.domain.model.ResumenEmpresa;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/** Salida de GET /api/admin/panel. Las fechas se muestran en hora de Bolivia. */
public record PanelResponse(
        Resumen resumen,
        List<Dia> porDia,
        List<Entidad> topEntidades,
        List<Reciente> ultimosAnalisis,
        List<Empresa> porEmpresa
) {

    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");

    public record Resumen(long totalAnalisis, long totalPhishing, BigDecimal porcentajePhishing,
                          long alertasAbiertas, long usuariosActivos) {}

    public record Dia(LocalDate dia, long total, long phishing, long sospechosos, long seguros) {}

    public record Entidad(String entidad, String tipo, long veces) {}

    public record Reciente(UUID id, String contenido, String dominio, String resultado,
                           BigDecimal nivelRiesgo, OffsetDateTime fecha, String usuario, String empresa) {}

    public record Empresa(String empresa, long totalAnalisis, long totalPhishing,
                          BigDecimal riesgoPromedio, OffsetDateTime ultimoAnalisis) {}

    public static PanelResponse desde(Panel panel) {
        var r = panel.resumen();
        return new PanelResponse(
                new Resumen(r.totalAnalisis(), r.totalPhishing(), r.porcentajePhishing(),
                        r.alertasAbiertas(), r.usuariosActivos()),
                panel.porDia().stream().map(PanelResponse::dia).toList(),
                panel.topEntidades().stream().map(PanelResponse::entidad).toList(),
                panel.ultimosAnalisis().stream().map(PanelResponse::reciente).toList(),
                panel.porEmpresa().stream().map(PanelResponse::empresa).toList());
    }

    private static Dia dia(AnalisisDia d) {
        return new Dia(d.dia(), d.total(), d.phishing(), d.sospechosos(), d.seguros());
    }

    private static Entidad entidad(EntidadTop e) {
        return new Entidad(e.entidad(), e.tipo(), e.veces());
    }

    private static Reciente reciente(AnalisisReciente a) {
        return new Reciente(a.id(), a.contenido(), a.dominio(), a.resultado(),
                a.nivelRiesgo(), enZonaLocal(a.fecha()), a.usuario(), a.empresa());
    }

    private static Empresa empresa(ResumenEmpresa e) {
        return new Empresa(e.empresa(), e.totalAnalisis(), e.totalPhishing(),
                e.riesgoPromedio(), enZonaLocal(e.ultimoAnalisis()));
    }

    private static OffsetDateTime enZonaLocal(OffsetDateTime fecha) {
        return fecha == null ? null : fecha.atZoneSameInstant(ZONA).toOffsetDateTime();
    }
}
