package bo.phishshield.backend.historial.application;

import bo.phishshield.backend.historial.domain.model.AnalisisNoEncontradoException;
import bo.phishshield.backend.historial.domain.model.DetalleAnalisis;
import bo.phishshield.backend.historial.domain.port.out.HistorialQueryPort;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;

import java.util.UUID;

/**
 * Caso de uso: abrir un analisis del historial con sus indicadores.
 * Si el analisis es de otra empresa responde igual que si no existiera.
 */
public class ObtenerDetalleAnalisisUseCase {

    private final HistorialQueryPort consultas;

    public ObtenerDetalleAnalisisUseCase(HistorialQueryPort consultas) {
        this.consultas = consultas;
    }

    public DetalleAnalisis ejecutar(UsuarioActual quien, UUID id) {
        AlcanceDatos alcance = AlcanceDatos.administrativo(quien);
        return consultas.detalle(id)
                .filter(d -> alcance.incluye(d.idEmpresa()))
                .orElseThrow(() -> new AnalisisNoEncontradoException(id));
    }
}
