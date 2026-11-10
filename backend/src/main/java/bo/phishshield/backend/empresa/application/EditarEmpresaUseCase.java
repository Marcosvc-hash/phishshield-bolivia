package bo.phishshield.backend.empresa.application;

import bo.phishshield.backend.empresa.domain.model.Empresa;
import bo.phishshield.backend.empresa.domain.model.EmpresaNoEncontradaException;
import bo.phishshield.backend.empresa.domain.model.EmpresaYaExisteException;
import bo.phishshield.backend.empresa.domain.port.out.EmpresaRepositoryPort;
import bo.phishshield.backend.shared.domain.model.AccionAuditoria;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import bo.phishshield.backend.shared.domain.port.out.AuditoriaPort;

import java.util.UUID;

/**
 * Caso de uso: corregir los datos de una empresa (solo ADMIN).
 * El NIT sigue siendo unico, sin contar a la propia empresa: guardarla
 * sin cambiar su NIT no es un duplicado.
 */
public class EditarEmpresaUseCase {

    private final EmpresaRepositoryPort empresaRepository;
    private final AuditoriaPort auditoria;

    public EditarEmpresaUseCase(EmpresaRepositoryPort empresaRepository, AuditoriaPort auditoria) {
        this.empresaRepository = empresaRepository;
        this.auditoria = auditoria;
    }

    public Empresa ejecutar(UsuarioActual quien, UUID id, ComandoCrearEmpresa datos, String ip) {
        AlcanceDatos.soloPlataforma(quien);

        Empresa empresa = empresaRepository.buscarPorUuid(id)
                .orElseThrow(() -> new EmpresaNoEncontradaException(id));
        empresa.actualizarDatos(datos.nombre(), datos.nit(), datos.sector(), datos.ciudad(), datos.plan());

        if (empresa.getNit() != null && empresaRepository.existeNitEnOtra(empresa.getNit(), empresa.getId())) {
            throw new EmpresaYaExisteException(empresa.getNit());
        }

        Empresa guardada = empresaRepository.guardar(empresa);
        auditoria.registrar(AuditoriaEmpresa.evento(AccionAuditoria.EMPRESA_EDITADA, quien, guardada, ip));
        return guardada;
    }
}
