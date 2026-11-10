package bo.phishshield.backend.panel.application;

import bo.phishshield.backend.panel.domain.model.AnalisisDia;
import bo.phishshield.backend.panel.domain.model.AnalisisReciente;
import bo.phishshield.backend.panel.domain.model.EntidadTop;
import bo.phishshield.backend.panel.domain.model.Panel;
import bo.phishshield.backend.panel.domain.model.ResumenEmpresa;
import bo.phishshield.backend.panel.domain.model.ResumenPanel;
import bo.phishshield.backend.panel.domain.port.out.PanelQueryPort;
import bo.phishshield.backend.shared.domain.model.AccesoDenegadoException;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.Rol;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ObtenerPanelUseCaseTest {

    /** Doble en memoria: anota con que alcance lo llamaron y devuelve datos fijos. */
    static class ConsultasFalsas implements PanelQueryPort {
        final List<AlcanceDatos> alcancesUsados = new ArrayList<>();
        LocalDate desdePedido;

        @Override public ResumenPanel resumen(AlcanceDatos alcance) {
            alcancesUsados.add(alcance);
            return new ResumenPanel(8, 2, 3, 4);
        }
        @Override public List<AnalisisDia> analisisPorDia(AlcanceDatos alcance, LocalDate desde) {
            desdePedido = desde;
            return List.of(new AnalisisDia(LocalDate.of(2026, 11, 8), 5, 2, 1, 2));
        }
        @Override public List<EntidadTop> topEntidades(AlcanceDatos alcance, int limite) {
            return List.of(new EntidadTop("Banco Nacional de Bolivia", "banco", 3));
        }
        @Override public List<AnalisisReciente> ultimosAnalisis(AlcanceDatos alcance, int limite) {
            return List.of();
        }
        @Override public List<ResumenEmpresa> resumenPorEmpresa() {
            return List.of(new ResumenEmpresa("Importadora Andina", 8, 2, BigDecimal.TEN, null));
        }
    }

    // 2026-11-09 15:00 en La Paz (19:00 UTC)
    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-09T19:00:00Z"), ZoneOffset.UTC);
    private final ConsultasFalsas consultas = new ConsultasFalsas();
    private final ObtenerPanelUseCase casoDeUso = new ObtenerPanelUseCase(consultas, reloj);

    private static UsuarioActual con(Rol rol, Long idEmpresa) {
        return new UsuarioActual(1L, UUID.randomUUID(), "x@x.bo", idEmpresa, rol);
    }

    @Test
    void el_admin_ve_toda_la_plataforma_y_el_resumen_por_empresa() {
        Panel panel = casoDeUso.ejecutar(con(Rol.ADMIN, null), 30);

        assertTrue(consultas.alcancesUsados.get(0).esGlobal());
        assertEquals(1, panel.porEmpresa().size());
        assertEquals(new BigDecimal("25.0"), panel.resumen().porcentajePhishing());
    }

    @Test
    void el_admin_de_empresa_solo_ve_su_empresa_y_no_compara_empresas() {
        Panel panel = casoDeUso.ejecutar(con(Rol.ADMIN_EMPRESA, 7L), 30);

        assertEquals(7L, consultas.alcancesUsados.get(0).idEmpresa());
        assertTrue(panel.porEmpresa().isEmpty());
    }

    @Test
    void un_usuario_comun_no_puede_ver_el_panel() {
        assertThrows(AccesoDenegadoException.class,
                () -> casoDeUso.ejecutar(con(Rol.USUARIO, 7L), 30));
        assertTrue(consultas.alcancesUsados.isEmpty());
    }

    @Test
    void el_grafico_trae_todos_los_dias_y_rellena_con_cero_los_vacios() {
        Panel panel = casoDeUso.ejecutar(con(Rol.ADMIN, null), 7);

        assertEquals(LocalDate.of(2026, 11, 3), consultas.desdePedido);
        assertEquals(7, panel.porDia().size());
        assertEquals(LocalDate.of(2026, 11, 9), panel.porDia().get(6).dia());
        assertEquals(5, panel.porDia().get(5).total());   // 8 de noviembre: con datos
        assertEquals(0, panel.porDia().get(0).total());   // 3 de noviembre: vacio
    }

    @Test
    void sin_analisis_el_porcentaje_es_cero_y_no_divide_entre_cero() {
        assertEquals(new BigDecimal("0.0"), new ResumenPanel(0, 0, 0, 0).porcentajePhishing());
    }
}
