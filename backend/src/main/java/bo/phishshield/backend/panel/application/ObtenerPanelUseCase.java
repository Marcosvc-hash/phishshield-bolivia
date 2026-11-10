package bo.phishshield.backend.panel.application;

import bo.phishshield.backend.panel.domain.model.AnalisisDia;
import bo.phishshield.backend.panel.domain.model.Panel;
import bo.phishshield.backend.panel.domain.port.out.PanelQueryPort;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Caso de uso: armar el panel segun quien lo pide.
 * ADMIN ve toda la plataforma; ADMIN_EMPRESA, solo su empresa; USUARIO, nada.
 */
public class ObtenerPanelUseCase {

    public static final int DIAS_POR_DEFECTO = 30;
    public static final int DIAS_MAXIMOS = 90;
    private static final int LIMITE_TOP_ENTIDADES = 5;
    private static final int LIMITE_ULTIMOS = 10;

    /** El "dia" se cuenta en hora de Bolivia, igual que la vista v_analisis_por_dia. */
    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");

    private final PanelQueryPort consultas;
    private final Clock reloj;

    public ObtenerPanelUseCase(PanelQueryPort consultas, Clock reloj) {
        this.consultas = consultas;
        this.reloj = reloj;
    }

    public Panel ejecutar(UsuarioActual quien, int dias) {
        AlcanceDatos alcance = AlcanceDatos.administrativo(quien);
        int cantidadDias = Math.min(Math.max(1, dias), DIAS_MAXIMOS);

        LocalDate hoy = LocalDate.now(reloj.withZone(ZONA));
        LocalDate desde = hoy.minusDays(cantidadDias - 1L);

        return new Panel(
                consultas.resumen(alcance),
                completarDias(consultas.analisisPorDia(alcance, desde), desde, hoy),
                consultas.topEntidades(alcance, LIMITE_TOP_ENTIDADES),
                consultas.ultimosAnalisis(alcance, LIMITE_ULTIMOS),
                alcance.esGlobal() ? consultas.resumenPorEmpresa() : List.of());
    }

    /**
     * La base solo devuelve los dias que tuvieron analisis. El grafico necesita
     * todos, asi que los dias sin actividad se rellenan con cero.
     */
    private static List<AnalisisDia> completarDias(List<AnalisisDia> conDatos,
                                                   LocalDate desde, LocalDate hasta) {
        Map<LocalDate, AnalisisDia> porFecha = conDatos.stream()
                .collect(Collectors.toMap(AnalisisDia::dia, Function.identity()));

        List<AnalisisDia> todos = new ArrayList<>();
        for (LocalDate dia = desde; !dia.isAfter(hasta); dia = dia.plusDays(1)) {
            todos.add(porFecha.getOrDefault(dia, AnalisisDia.vacio(dia)));
        }
        return todos;
    }
}
