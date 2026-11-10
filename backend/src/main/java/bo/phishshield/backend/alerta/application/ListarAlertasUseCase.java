package bo.phishshield.backend.alerta.application;

import bo.phishshield.backend.alerta.domain.model.AlertaFila;
import bo.phishshield.backend.alerta.domain.model.FiltroAlertas;
import bo.phishshield.backend.alerta.domain.port.out.AlertaRepositoryPort;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.Pagina;
import bo.phishshield.backend.shared.domain.model.SolicitudPagina;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;

/**
 * Caso de uso: bandeja de alertas y contador de pendientes.
 * ADMIN ve todas; ADMIN_EMPRESA, las de los analisis de su empresa.
 */
public class ListarAlertasUseCase {

    private final AlertaRepositoryPort alertas;

    public ListarAlertasUseCase(AlertaRepositoryPort alertas) {
        this.alertas = alertas;
    }

    public Pagina<AlertaFila> ejecutar(UsuarioActual quien, FiltroAlertas filtro, SolicitudPagina pagina) {
        return alertas.buscar(AlcanceDatos.administrativo(quien), filtro, pagina);
    }

    /** Para el numero junto a "Alertas" en el menu lateral. */
    public long contarPendientes(UsuarioActual quien) {
        return alertas.contarPendientes(AlcanceDatos.administrativo(quien));
    }
}
