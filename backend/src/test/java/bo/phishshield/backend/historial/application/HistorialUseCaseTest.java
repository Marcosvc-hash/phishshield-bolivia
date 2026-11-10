package bo.phishshield.backend.historial.application;

import bo.phishshield.backend.analisis.domain.model.Resultado;
import bo.phishshield.backend.historial.domain.model.AnalisisFila;
import bo.phishshield.backend.historial.domain.model.AnalisisNoEncontradoException;
import bo.phishshield.backend.historial.domain.model.DetalleAnalisis;
import bo.phishshield.backend.historial.domain.model.FiltroHistorial;
import bo.phishshield.backend.historial.domain.port.out.HistorialQueryPort;
import bo.phishshield.backend.shared.domain.model.AccesoDenegadoException;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.Pagina;
import bo.phishshield.backend.shared.domain.model.Rol;
import bo.phishshield.backend.shared.domain.model.SolicitudPagina;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class HistorialUseCaseTest {

    private static final long ANDINA = 1L;
    private static final long OTRA = 2L;

    /** Doble en memoria: recuerda con que alcance lo llamaron. */
    static class HistorialEnMemoria implements HistorialQueryPort {
        final Map<UUID, DetalleAnalisis> detalles = new HashMap<>();
        AlcanceDatos ultimoAlcance;
        FiltroHistorial ultimoFiltro;

        @Override
        public Pagina<AnalisisFila> buscar(AlcanceDatos alcance, FiltroHistorial filtro, SolicitudPagina pagina) {
            ultimoAlcance = alcance;
            ultimoFiltro = filtro;
            return Pagina.de(List.of(), pagina, 0);
        }

        @Override
        public Optional<DetalleAnalisis> detalle(UUID id) {
            return Optional.ofNullable(detalles.get(id));
        }

        UUID agregar(Long idEmpresa) {
            UUID id = UUID.randomUUID();
            detalles.put(id, new DetalleAnalisis(id, idEmpresa, "http://x.xyz", "x.xyz", "phishing",
                    new BigDecimal("82.00"), "web", 12, OffsetDateTime.now(), null, null,
                    "phishshield-url-ia 1.0.0", "pendiente", "alta",
                    List.of(new DetalleAnalisis.IndicadorDetalle("IA_MODELO_URL", "Modelo de IA",
                            "probabilidad 0.91", new BigDecimal("36.40")))));
            return id;
        }
    }

    private final HistorialEnMemoria consultas = new HistorialEnMemoria();
    private final ListarHistorialUseCase listar = new ListarHistorialUseCase(consultas);
    private final ObtenerDetalleAnalisisUseCase detalle = new ObtenerDetalleAnalisisUseCase(consultas);

    private static UsuarioActual con(Rol rol, Long idEmpresa) {
        return new UsuarioActual(9L, UUID.randomUUID(), "quien@x.bo", idEmpresa, rol);
    }

    private static final FiltroHistorial SIN_FILTRO = new FiltroHistorial(null, null, null);

    @Test
    void el_admin_consulta_con_alcance_global() {
        listar.ejecutar(con(Rol.ADMIN, null), SIN_FILTRO, new SolicitudPagina(0, 20));
        assertTrue(consultas.ultimoAlcance.esGlobal());
    }

    @Test
    void el_admin_de_empresa_consulta_solo_su_empresa() {
        listar.ejecutar(con(Rol.ADMIN_EMPRESA, ANDINA), SIN_FILTRO, new SolicitudPagina(0, 20));
        assertEquals(ANDINA, consultas.ultimoAlcance.idEmpresa());
    }

    @Test
    void un_usuario_comun_no_ve_el_historial() {
        assertThrows(AccesoDenegadoException.class,
                () -> listar.ejecutar(con(Rol.USUARIO, ANDINA), SIN_FILTRO, new SolicitudPagina(0, 20)));
    }

    @Test
    void el_filtro_normaliza_el_texto_y_el_resultado() {
        FiltroHistorial filtro = new FiltroHistorial(
                FiltroHistorial.resultadoDesdeTexto(" phishing "), null, "  bnb  ");

        assertEquals(Resultado.PHISHING, filtro.resultado());
        assertEquals("bnb", filtro.texto());
        assertNull(new FiltroHistorial(null, null, "   ").texto());
        assertNull(FiltroHistorial.resultadoDesdeTexto(""));
        assertThrows(IllegalArgumentException.class, () -> FiltroHistorial.resultadoDesdeTexto("malo"));
    }

    @Test
    void el_detalle_trae_los_indicadores_incluida_la_ia() {
        UUID id = consultas.agregar(ANDINA);

        DetalleAnalisis d = detalle.ejecutar(con(Rol.ADMIN_EMPRESA, ANDINA), id);

        assertEquals("IA_MODELO_URL", d.indicadores().get(0).codigo());
        assertEquals(new BigDecimal("36.40"), d.indicadores().get(0).aporte());
    }

    @Test
    void el_detalle_de_otra_empresa_responde_como_inexistente() {
        UUID id = consultas.agregar(OTRA);

        assertThrows(AnalisisNoEncontradoException.class,
                () -> detalle.ejecutar(con(Rol.ADMIN_EMPRESA, ANDINA), id));
    }

    @Test
    void un_analisis_anonimo_solo_lo_ve_el_admin() {
        UUID id = consultas.agregar(null);

        assertNotNull(detalle.ejecutar(con(Rol.ADMIN, null), id));
        assertThrows(AnalisisNoEncontradoException.class,
                () -> detalle.ejecutar(con(Rol.ADMIN_EMPRESA, ANDINA), id));
    }
}
