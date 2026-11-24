package bo.phishshield.backend.qa.application;

import bo.phishshield.backend.qa.dobles.DoblesQa;
import bo.phishshield.backend.qa.dobles.DoblesQa.EjecucionesEnMemoria;
import bo.phishshield.backend.qa.domain.model.AlcanceEjecucion;
import bo.phishshield.backend.qa.domain.model.ArchivoEvidencia;
import bo.phishshield.backend.qa.domain.model.ArchivosReporte;
import bo.phishshield.backend.qa.domain.model.CorreoDestino;
import bo.phishshield.backend.qa.domain.model.DesenlaceCaso;
import bo.phishshield.backend.qa.domain.model.Ejecucion;
import bo.phishshield.backend.qa.domain.model.EvidenciaNoEncontradaException;
import bo.phishshield.backend.qa.domain.model.FormatoReporte;
import bo.phishshield.backend.qa.domain.model.ReporteNoDisponibleException;
import bo.phishshield.backend.qa.domain.model.Suite;
import bo.phishshield.backend.qa.domain.port.out.EvidenciasPort;
import bo.phishshield.backend.qa.domain.port.out.GeneradorReportePort;
import bo.phishshield.backend.shared.domain.model.AccesoDenegadoException;
import bo.phishshield.backend.shared.domain.model.Rol;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** PublicarResultados y ObtenerReporte con un generador y un disco en memoria. */
class ReportesQaTest {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-24T10:00:00-04:00");
    private static final UsuarioActual ADMIN =
            new UsuarioActual(1L, UUID.randomUUID(), "admin@phishshield.bo", null, Rol.ADMIN);

    /** "Disco" en memoria: el generador escribe aqui y EvidenciasPort lee de aqui. */
    private final Map<String, byte[]> disco = new HashMap<>();
    private int generados;
    private boolean generadorRoto;

    private final GeneradorReportePort generador = e -> {
        if (generadorRoto) {
            throw new IllegalStateException("disco lleno");
        }
        generados++;
        String pdf = e.getUuidPublico() + "/reporte-detallado.pdf";
        String html = e.getUuidPublico() + "/reporte-resumen.html";
        disco.put(pdf, "%PDF".getBytes(StandardCharsets.UTF_8));
        disco.put(html, "<!doctype html>".getBytes(StandardCharsets.UTF_8));
        return new ArchivosReporte(pdf, html);
    };
    private final EvidenciasPort evidencias = ruta -> Optional.ofNullable(disco.get(ruta))
            .map(bytes -> new ArchivoEvidencia(ruta.substring(ruta.indexOf('/') + 1), "application/octet-stream", bytes));

    private final EjecucionesEnMemoria ejecuciones = new EjecucionesEnMemoria();

    private Ejecucion guardada(boolean terminada) {
        Ejecucion e = Ejecucion.nueva(1L, Suite.SMOKE, AlcanceEjecucion.SUITE_COMPLETA,
                new CorreoDestino("qa@phishshield.bo"), "qa", Map.of(),
                List.of(DoblesQa.caso("TC-SMK-001", Suite.SMOKE, 1)), AHORA);
        Ejecucion conId = ejecuciones.guardar(e);
        if (terminada) {
            conId.iniciar(AHORA);
            conId.registrarResultado(DesenlaceCaso.aprobado("TC-SMK-001", AHORA, AHORA.plusSeconds(1), List.of(), List.of()));
            conId.finalizar(AHORA.plusSeconds(2));
            ejecuciones.guardar(conId);
        }
        return conId;
    }

    @Test
    void al_terminar_se_generan_y_se_registran_los_reportes() {
        Ejecucion e = guardada(true);

        new PublicarResultadosUseCase(ejecuciones, generador).alTerminar(e);

        Ejecucion despues = ejecuciones.buscarPorUuid(e.getUuidPublico()).orElseThrow();
        assertTrue(despues.tieneReportes());
        assertEquals(e.getUuidPublico() + "/reporte-detallado.pdf", despues.getRutaReportePdf());
    }

    @Test
    void si_el_reporte_falla_no_se_propaga_el_error() {
        Ejecucion e = guardada(true);
        generadorRoto = true;

        assertDoesNotThrow(() -> new PublicarResultadosUseCase(ejecuciones, generador).alTerminar(e));
        assertFalse(ejecuciones.buscarPorUuid(e.getUuidPublico()).orElseThrow().tieneReportes());
    }

    @Test
    void se_descarga_el_reporte_ya_generado_sin_volver_a_generarlo() {
        Ejecucion e = guardada(true);
        new PublicarResultadosUseCase(ejecuciones, generador).alTerminar(e);
        ObtenerReporteUseCase obtener = new ObtenerReporteUseCase(ejecuciones, generador, evidencias);

        ArchivoEvidencia pdf = obtener.ejecutar(ADMIN, e.getUuidPublico(), FormatoReporte.PDF);
        ArchivoEvidencia html = obtener.ejecutar(ADMIN, e.getUuidPublico(), FormatoReporte.HTML);

        assertEquals("reporte-detallado.pdf", pdf.nombre());
        assertEquals("reporte-resumen.html", html.nombre());
        assertEquals(1, generados);
    }

    @Test
    void si_el_archivo_no_existe_se_genera_al_pedirlo() {
        Ejecucion e = guardada(true);
        ObtenerReporteUseCase obtener = new ObtenerReporteUseCase(ejecuciones, generador, evidencias);

        ArchivoEvidencia pdf = obtener.ejecutar(ADMIN, e.getUuidPublico(), FormatoReporte.PDF);

        assertEquals("reporte-detallado.pdf", pdf.nombre());
        assertEquals(1, generados);
        assertTrue(ejecuciones.buscarPorUuid(e.getUuidPublico()).orElseThrow().tieneReportes());
    }

    @Test
    void mientras_la_ejecucion_esta_activa_no_hay_reporte() {
        Ejecucion e = guardada(false);
        ObtenerReporteUseCase obtener = new ObtenerReporteUseCase(ejecuciones, generador, evidencias);

        assertThrows(ReporteNoDisponibleException.class,
                () -> obtener.ejecutar(ADMIN, e.getUuidPublico(), FormatoReporte.PDF));
        assertEquals(0, generados);
    }

    @Test
    void solo_se_entregan_capturas_png_de_la_misma_ejecucion() {
        UUID id = UUID.randomUUID();
        disco.put(id + "/capturas/TC-SMK-005-01-login.png", new byte[]{1});
        disco.put(id + "/reporte-detallado.pdf", new byte[]{2});
        ObtenerEvidenciaUseCase obtener = new ObtenerEvidenciaUseCase(evidencias);

        assertEquals(1, obtener.ejecutar(ADMIN, id, id + "/capturas/TC-SMK-005-01-login.png").contenido().length);
        assertThrows(EvidenciaNoEncontradaException.class,
                () -> obtener.ejecutar(ADMIN, id, id + "/reporte-detallado.pdf"), "No es una captura");
        assertThrows(EvidenciaNoEncontradaException.class,
                () -> obtener.ejecutar(ADMIN, UUID.randomUUID(), id + "/capturas/TC-SMK-005-01-login.png"), "Otra ejecución");
        assertThrows(EvidenciaNoEncontradaException.class,
                () -> obtener.ejecutar(ADMIN, id, id + "/capturas/../../otra/x.png"), "Sale de la carpeta");
        assertThrows(EvidenciaNoEncontradaException.class,
                () -> obtener.ejecutar(ADMIN, id, id + "/capturas/no-existe.png"));
    }

    @Test
    void solo_el_admin_descarga_reportes() {
        UsuarioActual empresa = new UsuarioActual(2L, UUID.randomUUID(), "m@andina.bo", 1L, Rol.ADMIN_EMPRESA);
        ObtenerReporteUseCase obtener = new ObtenerReporteUseCase(ejecuciones, generador, evidencias);

        assertThrows(AccesoDenegadoException.class,
                () -> obtener.ejecutar(empresa, UUID.randomUUID(), FormatoReporte.PDF));
    }
}
