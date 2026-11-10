package bo.phishshield.backend.empresa.application;

import bo.phishshield.backend.empresa.domain.model.Empresa;
import bo.phishshield.backend.empresa.domain.model.EmpresaNoEncontradaException;
import bo.phishshield.backend.empresa.domain.model.EmpresaYaExisteException;
import bo.phishshield.backend.shared.dobles.AuditoriaEnMemoria;
import bo.phishshield.backend.shared.domain.model.AccesoDenegadoException;
import bo.phishshield.backend.shared.domain.model.AccionAuditoria;
import bo.phishshield.backend.shared.domain.model.Rol;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static bo.phishshield.backend.empresa.application.CrearEmpresaUseCaseTest.ADMIN;
import static bo.phishshield.backend.empresa.application.CrearEmpresaUseCaseTest.IP;
import static org.junit.jupiter.api.Assertions.*;

class EditarEmpresaUseCaseTest {

    private final CrearEmpresaUseCaseTest.RepositorioEnMemoria repositorio = new CrearEmpresaUseCaseTest.RepositorioEnMemoria();
    private final AuditoriaEnMemoria auditoria = new AuditoriaEnMemoria();
    private final CrearEmpresaUseCase crear = new CrearEmpresaUseCase(repositorio, auditoria);
    private final EditarEmpresaUseCase editar = new EditarEmpresaUseCase(repositorio, auditoria);
    private final CambiarEstadoEmpresaUseCase cambiarEstado = new CambiarEstadoEmpresaUseCase(repositorio, auditoria);

    private Empresa andina;
    private Empresa sanRafael;

    {
        andina = crear.ejecutar(ADMIN, new ComandoCrearEmpresa("Importadora Andina", "1023456789", "Comercio", "La Paz", null), IP);
        sanRafael = crear.ejecutar(ADMIN, new ComandoCrearEmpresa("Clinica San Rafael", "2034567890", "Salud", "Cochabamba", "profesional"), IP);
        auditoria.eventos.clear();
    }

    private static ComandoCrearEmpresa datos(String nombre, String nit, String plan) {
        return new ComandoCrearEmpresa(nombre, nit, "Comercio", "El Alto", plan);
    }

    @Test
    void edita_la_misma_fila_sin_crear_otra() {
        Empresa editada = editar.ejecutar(ADMIN, andina.getUuidPublico(),
                datos("Importadora Andina S.R.L.", "1023456789", "empresarial"), IP);

        assertEquals(2, repositorio.guardadas.size(), "Editar no debe duplicar la empresa");
        assertEquals(andina.getUuidPublico(), editada.getUuidPublico());
        assertEquals("Importadora Andina S.R.L.", editada.getNombre());
        assertEquals("El Alto", editada.getCiudad());
        assertEquals("empresarial", editada.getPlan());
        assertEquals(List.of(AccionAuditoria.EMPRESA_EDITADA), auditoria.acciones());
    }

    @Test
    void guardar_con_su_propio_nit_no_es_duplicado_pero_el_de_otra_si() {
        assertDoesNotThrow(() -> editar.ejecutar(ADMIN, andina.getUuidPublico(), datos("Andina", "1023456789", null), IP));

        assertThrows(EmpresaYaExisteException.class,
                () -> editar.ejecutar(ADMIN, andina.getUuidPublico(), datos("Andina", "2034567890", null), IP));
    }

    @Test
    void un_nit_con_letras_se_rechaza_en_el_dominio() {
        assertThrows(IllegalArgumentException.class,
                () -> editar.ejecutar(ADMIN, andina.getUuidPublico(), datos("Andina", "12AB567", null), IP));
    }

    @Test
    void sin_plan_conserva_el_que_tenia() {
        Empresa editada = editar.ejecutar(ADMIN, sanRafael.getUuidPublico(), datos("Clinica San Rafael", null, null), IP);
        assertEquals("profesional", editada.getPlan());
    }

    @Test
    void desactivar_y_reactivar_se_auditan_y_repetirlo_no_hace_nada() {
        // El doble devuelve el mismo objeto en cada llamada: se revisa justo despues de cada una
        assertFalse(cambiarEstado.ejecutar(ADMIN, andina.getUuidPublico(), false, IP).isActiva());
        assertFalse(cambiarEstado.ejecutar(ADMIN, andina.getUuidPublico(), false, IP).isActiva());
        assertTrue(cambiarEstado.ejecutar(ADMIN, andina.getUuidPublico(), true, IP).isActiva());

        assertEquals(List.of(AccionAuditoria.EMPRESA_DESACTIVADA, AccionAuditoria.EMPRESA_ACTIVADA), auditoria.acciones());
    }

    @Test
    void una_empresa_inexistente_da_404() {
        assertThrows(EmpresaNoEncontradaException.class,
                () -> cambiarEstado.ejecutar(ADMIN, UUID.randomUUID(), false, IP));
    }

    @Test
    void el_admin_de_empresa_no_edita_empresas_ni_la_suya() {
        UsuarioActual maria = new UsuarioActual(2L, UUID.randomUUID(), "m@x.bo", andina.getId(), Rol.ADMIN_EMPRESA);

        assertThrows(AccesoDenegadoException.class,
                () -> editar.ejecutar(maria, andina.getUuidPublico(), datos("Hackeada", null, null), IP));
        assertThrows(AccesoDenegadoException.class,
                () -> cambiarEstado.ejecutar(maria, andina.getUuidPublico(), false, IP));
    }
}
