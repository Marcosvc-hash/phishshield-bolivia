package bo.phishshield.backend.empresa.application;

import bo.phishshield.backend.empresa.domain.model.Empresa;
import bo.phishshield.backend.empresa.domain.model.EmpresaNoEncontradaException;
import bo.phishshield.backend.empresa.domain.port.out.EmpresaRepositoryPort;
import bo.phishshield.backend.shared.domain.model.AccionAuditoria;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import bo.phishshield.backend.shared.domain.port.out.AuditoriaPort;

import java.util.UUID;

/**
 * Caso de uso: activar o desactivar una empresa (solo ADMIN).
 * Que significa estar inactiva lo explica Empresa.desactivar().
 * Si ya esta en el estado pedido, no cambia nada ni se audita.
 */
public class CambiarEstadoEmpresaUseCase {

    private final EmpresaRepositoryPort empresaRepository;
    private final AuditoriaPort auditoria;

    public CambiarEstadoEmpresaUseCase(EmpresaRepositoryPort empresaRepository, AuditoriaPort auditoria) {
        this.empresaRepository = empresaRepository;
        this.auditoria = auditoria;
    }

    public Empresa ejecutar(UsuarioActual quien, UUID id, boolean activar, String ip) {
        AlcanceDatos.soloPlataforma(quien);

        Empresa empresa = empresaRepository.buscarPorUuid(id)
                .orElseThrow(() -> new EmpresaNoEncontradaException(id));
        if (empresa.isActiva() == activar) {
            return empresa;
        }

        if (activar) {
            empresa.activar();
        } else {
            empresa.desactivar();
        }
        Empresa guardada = empresaRepository.guardar(empresa);
        auditoria.registrar(AuditoriaEmpresa.evento(
                activar ? AccionAuditoria.EMPRESA_ACTIVADA : AccionAuditoria.EMPRESA_DESACTIVADA,
                quien, guardada, ip));
        return guardada;
    }
}
