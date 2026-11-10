package bo.phishshield.backend.empresa.application;

import bo.phishshield.backend.empresa.domain.model.Empresa;
import bo.phishshield.backend.empresa.domain.model.EmpresaYaExisteException;
import bo.phishshield.backend.empresa.domain.port.out.EmpresaRepositoryPort;
import bo.phishshield.backend.shared.domain.model.AccionAuditoria;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import bo.phishshield.backend.shared.domain.port.out.AuditoriaPort;

/**
 * Caso de uso: registrar una empresa cliente (solo ADMIN).
 * Aplica la regla del NIT unico; las validaciones de los campos
 * viven en Empresa.
 */
public class CrearEmpresaUseCase {

    private final EmpresaRepositoryPort empresaRepository;
    private final AuditoriaPort auditoria;

    public CrearEmpresaUseCase(EmpresaRepositoryPort empresaRepository, AuditoriaPort auditoria) {
        this.empresaRepository = empresaRepository;
        this.auditoria = auditoria;
    }

    public Empresa ejecutar(UsuarioActual quien, ComandoCrearEmpresa comando, String ip) {
        AlcanceDatos.soloPlataforma(quien);

        Empresa nueva = Empresa.nueva(
                comando.nombre(),
                comando.nit(),
                comando.sector(),
                comando.ciudad(),
                comando.plan());

        // El NIT es opcional: varias empresas pueden registrarse sin el
        if (nueva.getNit() != null && empresaRepository.existeNit(nueva.getNit())) {
            throw new EmpresaYaExisteException(nueva.getNit());
        }

        Empresa guardada = empresaRepository.guardar(nueva);
        auditoria.registrar(AuditoriaEmpresa.evento(AccionAuditoria.EMPRESA_CREADA, quien, guardada, ip));
        return guardada;
    }
}
