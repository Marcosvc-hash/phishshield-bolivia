package bo.phishshield.backend.panel.domain.model;

import java.util.List;

/**
 * Todo lo que muestra el panel en una sola respuesta.
 *
 * @param porEmpresa vacio para ADMIN_EMPRESA: solo el ADMIN compara empresas
 */
public record Panel(ResumenPanel resumen,
                    List<AnalisisDia> porDia,
                    List<EntidadTop> topEntidades,
                    List<AnalisisReciente> ultimosAnalisis,
                    List<ResumenEmpresa> porEmpresa) {

    public Panel {
        porDia = List.copyOf(porDia);
        topEntidades = List.copyOf(topEntidades);
        ultimosAnalisis = List.copyOf(ultimosAnalisis);
        porEmpresa = List.copyOf(porEmpresa);
    }
}
