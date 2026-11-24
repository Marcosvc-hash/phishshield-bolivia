package bo.phishshield.backend.reporte.application;

import bo.phishshield.backend.reporte.domain.model.EmpresaDelReporte;
import bo.phishshield.backend.reporte.domain.model.PeriodoInvalidoException;
import bo.phishshield.backend.reporte.domain.model.PeriodoReporte;
import bo.phishshield.backend.reporte.domain.model.ReporteEmpresa;
import bo.phishshield.backend.reporte.domain.model.ReporteNoEncontradoException;
import bo.phishshield.backend.reporte.domain.model.TotalesReporte;
import bo.phishshield.backend.reporte.domain.port.out.ReporteRepositoryPort;
import bo.phishshield.backend.shared.dobles.AuditoriaEnMemoria;
import bo.phishshield.backend.shared.domain.model.AccesoDenegadoException;
import bo.phishshield.backend.shared.domain.model.AccionAuditoria;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.Pagina;
import bo.phishshield.backend.shared.domain.model.Rol;
import bo.phishshield.backend.shared.domain.model.SolicitudPagina;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Casos de uso de la HU-17 con un repositorio en memoria, sin Spring. */
class ReportesPorEmpresaTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-24T15:00:00Z"), ZoneOffset.ofHours(-4));
    private static final LocalDate HOY = LocalDate.of(2026, 11, 24);

    private static final EmpresaDelReporte ANDINA = new EmpresaDelReporte(1L, UUID.randomUUID(), "Importadora Andina", true);
    private static final EmpresaDelReporte RAFAEL = new EmpresaDelReporte(2L, UUID.randomUUID(), "Clínica San Rafael", true);

    private static final UsuarioActual ADMIN = new UsuarioActual(10L, UUID.randomUUID(), "admin@phishshield.bo", null, Rol.ADMIN);
    private static final UsuarioActual DE_ANDINA = new UsuarioActual(11L, UUID.randomUUID(), "maria@andina.bo", 1L, Rol.ADMIN_EMPRESA);
    private static final UsuarioActual USUARIO = new UsuarioActual(12L, UUID.randomUUID(), "carlos@andina.bo", 1L, Rol.USUARIO);

    /** Repositorio en memoria: totales fijos por empresa y upsert por empresa + periodo, como la tabla. */
    static class ReportesEnMemoria implements ReporteRepositoryPort {
        final Map<Long, EmpresaDelReporte> empresas = new LinkedHashMap<>();
        final Map<Long, TotalesReporte> totalesPorEmpresa = new LinkedHashMap<>();
        final List<ReporteEmpresa> guardados = new ArrayList<>();
        PeriodoReporte ultimoPeriodoCalculado;
        private long siguienteId = 1;

        ReportesEnMemoria(EmpresaDelReporte... empresas) {
            for (EmpresaDelReporte e : empresas) {
                this.empresas.put(e.id(), e);
            }
        }

        @Override
        public Optional<EmpresaDelReporte> buscarEmpresaPorUuid(UUID uuid) {
            return empresas.values().stream().filter(e -> e.uuidPublico().equals(uuid)).findFirst();
        }

        @Override
        public Optional<EmpresaDelReporte> buscarEmpresaPorId(long id) {
            return Optional.ofNullable(empresas.get(id));
        }

        @Override
        public TotalesReporte calcularTotales(long idEmpresa, PeriodoReporte periodo) {
            ultimoPeriodoCalculado = periodo;
            return totalesPorEmpresa.getOrDefault(idEmpresa, new TotalesReporte(0, 0, 0));
        }

        @Override
        public Guardado guardar(long idEmpresa, PeriodoReporte periodo, TotalesReporte totales, long idUsuario) {
            Optional<ReporteEmpresa> anterior = guardados.stream()
                    .filter(r -> r.empresa().id() == idEmpresa && r.periodo().equals(periodo)).findFirst();
            ReporteEmpresa nuevo = new ReporteEmpresa(anterior.map(ReporteEmpresa::id).orElse(siguienteId++),
                    anterior.map(ReporteEmpresa::uuidPublico).orElse(UUID.randomUUID()), empresas.get(idEmpresa), periodo,
                    totales, "usuario-" + idUsuario, OffsetDateTime.now(RELOJ));
            anterior.ifPresent(guardados::remove);
            guardados.add(nuevo);
            return new Guardado(nuevo, anterior.isEmpty());
        }

        @Override
        public Optional<ReporteEmpresa> buscarPorUuid(UUID uuid) {
            return guardados.stream().filter(r -> r.uuidPublico().equals(uuid)).findFirst();
        }

        @Override
        public Pagina<ReporteEmpresa> listar(AlcanceDatos alcance, Long idEmpresa, SolicitudPagina pagina) {
            List<ReporteEmpresa> filas = guardados.stream()
                    .filter(r -> alcance.incluye(r.empresa().id()))
                    .filter(r -> idEmpresa == null || r.empresa().id() == idEmpresa)
                    .sorted(Comparator.comparing(ReporteEmpresa::id).reversed())
                    .toList();
            return Pagina.de(filas, pagina, filas.size());
        }
    }

    private final ReportesEnMemoria repo = new ReportesEnMemoria(ANDINA, RAFAEL);
    private final AuditoriaEnMemoria auditoria = new AuditoriaEnMemoria();
    private final GenerarReporteUseCase generar = new GenerarReporteUseCase(repo, auditoria, RELOJ);
    private final ListarReportesUseCase listar = new ListarReportesUseCase(repo);
    private final DescargarReportePdfUseCase descargar = new DescargarReportePdfUseCase(repo, r -> "%PDF".getBytes());

    private ReporteEmpresa generarComo(UsuarioActual quien, UUID empresa) {
        return generar.ejecutar(quien, new ComandoGenerarReporte(empresa, HOY.minusDays(29), HOY, "10.0.0.1"));
    }

    @Test
    void el_admin_genera_el_reporte_con_los_totales_de_la_base_y_queda_auditado() {
        repo.totalesPorEmpresa.put(1L, new TotalesReporte(40, 6, 9));

        ReporteEmpresa r = generarComo(ADMIN, ANDINA.uuidPublico());

        assertEquals("Importadora Andina", r.empresa().nombre());
        assertEquals(new TotalesReporte(40, 6, 9), r.totales());
        assertEquals(30, repo.ultimoPeriodoCalculado.dias());
        assertEquals(1, auditoria.eventos.size());
        assertEquals(AccionAuditoria.REPORTE_GENERADO, auditoria.eventos.getFirst().accion());
        assertEquals("reportes", auditoria.eventos.getFirst().entidad());
        assertEquals(true, auditoria.eventos.getFirst().detalle().get("nuevo"));
        assertEquals(40, auditoria.eventos.getFirst().detalle().get("total_analisis"));
    }

    @Test
    void generar_dos_veces_el_mismo_periodo_recalcula_sin_duplicar() {
        ReporteEmpresa primero = generarComo(ADMIN, ANDINA.uuidPublico());
        repo.totalesPorEmpresa.put(1L, new TotalesReporte(5, 1, 1));

        ReporteEmpresa segundo = generarComo(ADMIN, ANDINA.uuidPublico());

        assertEquals(primero.uuidPublico(), segundo.uuidPublico());
        assertEquals(5, segundo.totales().analisis());
        assertEquals(1, repo.guardados.size());
        assertEquals(false, auditoria.eventos.get(1).detalle().get("nuevo"));
    }

    @Test
    void el_admin_debe_elegir_la_empresa_y_una_inexistente_es_404() {
        assertThrows(IllegalArgumentException.class, () -> generarComo(ADMIN, null));
        assertThrows(ReporteNoEncontradoException.class, () -> generarComo(ADMIN, UUID.randomUUID()));
    }

    @Test
    void el_admin_de_empresa_solo_genera_el_de_la_suya() {
        assertEquals("Importadora Andina", generarComo(DE_ANDINA, null).empresa().nombre(), "Sin empresa: la suya");
        assertEquals("Importadora Andina", generarComo(DE_ANDINA, ANDINA.uuidPublico()).empresa().nombre());
        assertThrows(ReporteNoEncontradoException.class, () -> generarComo(DE_ANDINA, RAFAEL.uuidPublico()),
                "Otra empresa responde 404, igual que una inexistente");
    }

    @Test
    void un_usuario_comun_no_genera_reportes() {
        assertThrows(AccesoDenegadoException.class, () -> generarComo(USUARIO, null));
        assertThrows(AccesoDenegadoException.class, () -> listar.ejecutar(USUARIO, null, new SolicitudPagina(0, 20)));
    }

    @Test
    void un_periodo_que_termina_en_el_futuro_se_rechaza() {
        assertThrows(PeriodoInvalidoException.class, () -> generar.ejecutar(ADMIN,
                new ComandoGenerarReporte(ANDINA.uuidPublico(), HOY, HOY.plusDays(1), "ip")));
        assertTrue(auditoria.eventos.isEmpty());
    }

    @Test
    void el_listado_y_la_descarga_respetan_la_empresa_de_cada_quien() {
        ReporteEmpresa deAndina = generarComo(ADMIN, ANDINA.uuidPublico());
        ReporteEmpresa deRafael = generarComo(ADMIN, RAFAEL.uuidPublico());

        assertEquals(2, listar.ejecutar(ADMIN, null, new SolicitudPagina(0, 20)).totalElementos());
        assertEquals(1, listar.ejecutar(ADMIN, RAFAEL.uuidPublico(), new SolicitudPagina(0, 20)).totalElementos());
        assertEquals(List.of(deAndina.uuidPublico()), listar.ejecutar(DE_ANDINA, null, new SolicitudPagina(0, 20))
                .contenido().stream().map(ReporteEmpresa::uuidPublico).toList());
        assertThrows(ReporteNoEncontradoException.class,
                () -> listar.ejecutar(DE_ANDINA, RAFAEL.uuidPublico(), new SolicitudPagina(0, 20)));

        assertArrayEquals("%PDF".getBytes(), descargar.ejecutar(DE_ANDINA, deAndina.uuidPublico()).pdf());
        assertThrows(ReporteNoEncontradoException.class, () -> descargar.ejecutar(DE_ANDINA, deRafael.uuidPublico()));
    }
}
