package bo.phishshield.backend.panel.domain.port.out;

import bo.phishshield.backend.panel.domain.model.AnalisisDia;
import bo.phishshield.backend.panel.domain.model.AnalisisReciente;
import bo.phishshield.backend.panel.domain.model.EntidadTop;
import bo.phishshield.backend.panel.domain.model.ResumenEmpresa;
import bo.phishshield.backend.panel.domain.model.ResumenPanel;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;

import java.time.LocalDate;
import java.util.List;

/**
 * Consultas de solo lectura para el panel. Cada metodo respeta el alcance:
 * si es global ve todo; si no, solo la empresa del alcance.
 */
public interface PanelQueryPort {

    ResumenPanel resumen(AlcanceDatos alcance);

    /** Solo los dias que tuvieron analisis, desde la fecha indicada (incluida). */
    List<AnalisisDia> analisisPorDia(AlcanceDatos alcance, LocalDate desde);

    List<EntidadTop> topEntidades(AlcanceDatos alcance, int limite);

    List<AnalisisReciente> ultimosAnalisis(AlcanceDatos alcance, int limite);

    List<ResumenEmpresa> resumenPorEmpresa();
}
