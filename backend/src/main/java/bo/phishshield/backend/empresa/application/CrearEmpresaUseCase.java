package bo.phishshield.backend.empresa.application;

import bo.phishshield.backend.empresa.domain.model.Empresa;
import bo.phishshield.backend.empresa.domain.model.EmpresaYaExisteException;
import bo.phishshield.backend.empresa.domain.port.out.EmpresaRepositoryPort;

/**
 * Caso de uso: registrar una empresa cliente.
 * Aplica la regla del NIT unico; las validaciones de los campos
 * viven en el constructor de Empresa.
 */
public class CrearEmpresaUseCase {

    private final EmpresaRepositoryPort empresaRepository;

    public CrearEmpresaUseCase(EmpresaRepositoryPort empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    public Empresa ejecutar(ComandoCrearEmpresa comando) {
        // El NIT es opcional: varias empresas pueden registrarse sin el
        if (comando.nit() != null && empresaRepository.existeNit(comando.nit())) {
            throw new EmpresaYaExisteException(comando.nit());
        }

        Empresa nueva = Empresa.nueva(
                comando.nombre(),
                comando.nit(),
                comando.sector(),
                comando.ciudad(),
                comando.plan());

        return empresaRepository.guardar(nueva);
    }
}
