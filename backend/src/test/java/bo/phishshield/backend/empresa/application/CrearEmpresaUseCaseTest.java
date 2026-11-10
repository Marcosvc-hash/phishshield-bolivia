package bo.phishshield.backend.empresa.application;

import bo.phishshield.backend.empresa.domain.model.Empresa;
import bo.phishshield.backend.empresa.domain.model.EmpresaYaExisteException;
import bo.phishshield.backend.empresa.domain.port.out.EmpresaRepositoryPort;
import bo.phishshield.backend.shared.dobles.AuditoriaEnMemoria;
import bo.phishshield.backend.shared.domain.model.AccesoDenegadoException;
import bo.phishshield.backend.shared.domain.model.AccionAuditoria;
import bo.phishshield.backend.shared.domain.model.Rol;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CrearEmpresaUseCaseTest {

    // ---- Doble de prueba: repositorio en memoria ----

    static class RepositorioEnMemoria implements EmpresaRepositoryPort {
        final List<Empresa> guardadas = new ArrayList<>();

        @Override
        public List<Empresa> listarTodas() {
            return List.copyOf(guardadas);
        }

        @Override
        public Optional<Empresa> buscarPorId(Long id) {
            return guardadas.stream().filter(e -> id.equals(e.getId())).findFirst();
        }

        @Override
        public Optional<Empresa> buscarPorUuid(UUID uuidPublico) {
            return guardadas.stream().filter(e -> uuidPublico.equals(e.getUuidPublico())).findFirst();
        }

        /** Como el adaptador real: si ya tiene id, actualiza esa fila en vez de agregar otra. */
        @Override
        public Empresa guardar(Empresa empresa) {
            if (empresa.getId() != null) {
                guardadas.replaceAll(e -> e.getId().equals(empresa.getId()) ? empresa : e);
                return empresa;
            }
            Empresa conIdentidad = new Empresa(
                    (long) (guardadas.size() + 1), UUID.randomUUID(),
                    empresa.getNombre(), empresa.getNit(), empresa.getSector(),
                    empresa.getCiudad(), empresa.getPlan(), empresa.isActiva(),
                    OffsetDateTime.now());
            guardadas.add(conIdentidad);
            return conIdentidad;
        }

        @Override
        public boolean existeNit(String nit) {
            return nit != null && guardadas.stream().anyMatch(e -> nit.equals(e.getNit()));
        }

        @Override
        public boolean existeNitEnOtra(String nit, Long idEmpresa) {
            return nit != null && guardadas.stream()
                    .anyMatch(e -> nit.equals(e.getNit()) && !e.getId().equals(idEmpresa));
        }
    }

    static final UsuarioActual ADMIN = new UsuarioActual(1L, UUID.randomUUID(), "admin@x.bo", null, Rol.ADMIN);
    static final String IP = "190.129.10.25";

    private final RepositorioEnMemoria repositorio = new RepositorioEnMemoria();
    private final AuditoriaEnMemoria auditoria = new AuditoriaEnMemoria();
    private final CrearEmpresaUseCase casoDeUso = new CrearEmpresaUseCase(repositorio, auditoria);

    private Empresa crear(String nombre, String nit) {
        return casoDeUso.ejecutar(ADMIN, new ComandoCrearEmpresa(nombre, nit, "Comercio", "La Paz", null), IP);
    }

    // ---- Pruebas ----

    @Test
    void crea_la_empresa_con_plan_basico_por_defecto_y_la_audita() {
        Empresa creada = crear("Importadora Andina S.R.L.", "1023456789");

        assertEquals(1, repositorio.guardadas.size());
        assertNotNull(creada.getUuidPublico());
        assertEquals("basico", creada.getPlan());
        assertTrue(creada.isActiva());
        assertEquals(List.of(AccionAuditoria.EMPRESA_CREADA), auditoria.acciones());
    }

    @Test
    void rechaza_un_nit_duplicado() {
        crear("Importadora Andina S.R.L.", "1023456789");

        EmpresaYaExisteException ex = assertThrows(EmpresaYaExisteException.class,
                () -> crear("Otra empresa", "1023456789"));

        assertEquals("Ya existe una empresa registrada con el NIT 1023456789", ex.getMessage());
        assertEquals(1, repositorio.guardadas.size());
    }

    @Test
    void permite_varias_empresas_sin_nit_y_un_nit_vacio_cuenta_como_sin_nit() {
        crear("Tienda de barrio", null);
        crear("Otra tienda de barrio", "  ");

        assertEquals(2, repositorio.guardadas.size());
        assertNull(repositorio.guardadas.get(1).getNit());
    }

    @Test
    void un_nombre_vacio_lanza_illegal_argument() {
        assertThrows(IllegalArgumentException.class, () -> crear("  ", "1234567"));
        assertTrue(repositorio.guardadas.isEmpty());
    }

    @Test
    void solo_el_admin_crea_empresas() {
        UsuarioActual maria = new UsuarioActual(2L, UUID.randomUUID(), "m@x.bo", 1L, Rol.ADMIN_EMPRESA);

        assertThrows(AccesoDenegadoException.class, () -> casoDeUso.ejecutar(maria,
                new ComandoCrearEmpresa("X", null, null, null, null), IP));
    }
}
