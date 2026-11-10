package bo.phishshield.backend.listas.application;

import bo.phishshield.backend.listas.domain.model.ConflictoListaException;
import bo.phishshield.backend.listas.domain.model.DominioEnLista;
import bo.phishshield.backend.listas.domain.model.DominioFila;
import bo.phishshield.backend.listas.domain.model.EntidadSuplantada;
import bo.phishshield.backend.listas.domain.model.FiltroDominios;
import bo.phishshield.backend.listas.domain.model.NombreDominio;
import bo.phishshield.backend.listas.domain.model.TipoLista;
import bo.phishshield.backend.listas.domain.port.out.DominioListaRepositoryPort;
import bo.phishshield.backend.listas.domain.port.out.EntidadSuplantadaPort;
import bo.phishshield.backend.shared.dobles.AuditoriaEnMemoria;
import bo.phishshield.backend.shared.domain.model.AccesoDenegadoException;
import bo.phishshield.backend.shared.domain.model.AccionAuditoria;
import bo.phishshield.backend.shared.domain.model.Pagina;
import bo.phishshield.backend.shared.domain.model.Rol;
import bo.phishshield.backend.shared.domain.model.SolicitudPagina;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ListasUseCaseTest {

    /** Doble de la tabla dominios_lista: una lista de filas, sin DELETE. */
    static class DominiosEnMemoria implements DominioListaRepositoryPort {
        final List<DominioEnLista> filas = new ArrayList<>();

        @Override
        public Pagina<DominioFila> buscar(FiltroDominios filtro, SolicitudPagina pagina) {
            return Pagina.de(List.of(), pagina, 0);
        }

        @Override
        public Optional<DominioEnLista> buscarPorUuid(UUID id) {
            return filas.stream().filter(d -> d.getUuidPublico().equals(id)).findFirst();
        }

        @Override
        public Optional<DominioEnLista> buscar(NombreDominio dominio, TipoLista lista) {
            return filas.stream().filter(d -> d.getDominio().equals(dominio) && d.getLista() == lista).findFirst();
        }

        @Override
        public DominioEnLista guardar(DominioEnLista d) {
            if (d.getId() != null) {
                return d;   // el objeto ya es el de la lista: los cambios quedan
            }
            DominioEnLista conId = new DominioEnLista((long) filas.size() + 1, UUID.randomUUID(), d.getDominio(),
                    d.getLista(), d.getFuente(), d.getIdEntidad(), d.getMotivo(), d.isActivo());
            filas.add(conId);
            return conId;
        }

        @Override
        public Optional<DominioFila> buscarFila(UUID id) {
            return buscarPorUuid(id).map(d -> new DominioFila(d.getUuidPublico(), d.getDominio().valor(),
                    d.getLista().aValorBd(), null, d.getFuente(), d.getMotivo(), d.isActivo(), null));
        }

        DominioEnLista agregarExistente(String dominio, TipoLista lista, boolean activo) {
            DominioEnLista d = new DominioEnLista((long) filas.size() + 1, UUID.randomUUID(),
                    NombreDominio.de(dominio), lista, "semilla", null, "del seed", activo);
            filas.add(d);
            return d;
        }
    }

    static class EntidadesFijas implements EntidadSuplantadaPort {
        @Override
        public List<EntidadSuplantada> listar() {
            return List.of(new EntidadSuplantada("Banco Nacional de Bolivia", "banco", "bnb.com.bo"));
        }

        @Override
        public Optional<Long> idPorNombre(String nombre) {
            return "Banco Nacional de Bolivia".equals(nombre) ? Optional.of(1L) : Optional.empty();
        }
    }

    private static final UsuarioActual ADMIN = new UsuarioActual(1L, UUID.randomUUID(), "a@x.bo", null, Rol.ADMIN);
    private static final UsuarioActual MARIA = new UsuarioActual(2L, UUID.randomUUID(), "m@x.bo", 5L, Rol.ADMIN_EMPRESA);
    private static final String IP = "190.129.10.25";

    private final DominiosEnMemoria dominios = new DominiosEnMemoria();
    private final AuditoriaEnMemoria auditoria = new AuditoriaEnMemoria();
    private final EntidadesFijas entidades = new EntidadesFijas();
    private final AgregarDominioUseCase agregar = new AgregarDominioUseCase(dominios, entidades, auditoria);
    private final EditarDominioUseCase editar = new EditarDominioUseCase(dominios, entidades, auditoria);
    private final CambiarEstadoDominioUseCase cambiarEstado = new CambiarEstadoDominioUseCase(dominios, entidades, auditoria);
    private final ListarDominiosUseCase listar = new ListarDominiosUseCase(dominios, entidades);

    private static ComandoDominio comando(String dominio, String lista, String motivo, String entidad) {
        return new ComandoDominio(dominio, lista, motivo, entidad, IP);
    }

    @Test
    void agrega_a_la_lista_negra_normalizado_con_entidad_y_lo_audita() {
        DominioFila fila = agregar.ejecutar(ADMIN,
                comando(" BNB-Pagos.XYZ ", "negra", "Reportado por un cliente", "Banco Nacional de Bolivia"));

        assertEquals("bnb-pagos.xyz", fila.dominio());
        assertTrue(fila.activo());
        assertEquals(1L, dominios.filas.get(0).getIdEntidad());
        assertEquals("manual", dominios.filas.get(0).getFuente());

        assertEquals(List.of(AccionAuditoria.DOMINIO_AGREGADO), auditoria.acciones());
        Map<String, Object> detalle = auditoria.ultimo().detalle();
        assertEquals("bnb-pagos.xyz", detalle.get("dominio"));
        assertEquals("negra", detalle.get("lista"));
        assertTrue(auditoria.ultimo().accion().cambiaListasDeDominios(), "Debe invalidar la cache del motor");
    }

    @Test
    void un_dominio_ya_activo_en_esa_lista_es_conflicto() {
        dominios.agregarExistente("bnb-pagos.xyz", TipoLista.NEGRA, true);

        assertThrows(ConflictoListaException.class,
                () -> agregar.ejecutar(ADMIN, comando("bnb-pagos.xyz", "negra", "otra vez", null)));
        assertEquals(1, dominios.filas.size());
    }

    @Test
    void no_puede_estar_activo_en_la_blanca_y_en_la_negra_a_la_vez() {
        dominios.agregarExistente("visa.com", TipoLista.BLANCA, true);

        ConflictoListaException error = assertThrows(ConflictoListaException.class,
                () -> agregar.ejecutar(ADMIN, comando("visa.com", "negra", "error", null)));
        assertTrue(error.getMessage().contains("blanca"));
        assertTrue(auditoria.eventos.isEmpty());
    }

    @Test
    void agregar_uno_desactivado_reactiva_la_misma_fila() {
        DominioEnLista viejo = dominios.agregarExistente("bnb-pagos.xyz", TipoLista.NEGRA, false);

        agregar.ejecutar(ADMIN, comando("bnb-pagos.xyz", "negra", "Volvio a aparecer", null));

        assertEquals(1, dominios.filas.size(), "No debe crear otra fila (UNIQUE dominio, lista)");
        assertTrue(viejo.isActivo());
        assertEquals("Volvio a aparecer", viejo.getMotivo());
        assertEquals(List.of(AccionAuditoria.DOMINIO_REACTIVADO), auditoria.acciones());
    }

    @Test
    void eliminar_desactiva_sin_borrar_y_repetirlo_no_hace_nada() {
        DominioEnLista d = dominios.agregarExistente("bnb-pagos.xyz", TipoLista.NEGRA, true);

        DominioFila fila = cambiarEstado.ejecutar(ADMIN, d.getUuidPublico(), false, IP);
        cambiarEstado.ejecutar(ADMIN, d.getUuidPublico(), false, IP);

        assertFalse(fila.activo());
        assertEquals(1, dominios.filas.size(), "La fila sigue existiendo");
        assertEquals(List.of(AccionAuditoria.DOMINIO_DESACTIVADO), auditoria.acciones());
    }

    @Test
    void reactivar_revisa_el_choque_con_la_lista_opuesta() {
        DominioEnLista enNegra = dominios.agregarExistente("sitio.com", TipoLista.NEGRA, false);
        dominios.agregarExistente("sitio.com", TipoLista.BLANCA, true);

        assertThrows(ConflictoListaException.class,
                () -> cambiarEstado.ejecutar(ADMIN, enNegra.getUuidPublico(), true, IP));
        assertFalse(enNegra.isActivo());
    }

    @Test
    void editar_cambia_motivo_y_entidad() {
        DominioEnLista d = dominios.agregarExistente("bnb-pagos.xyz", TipoLista.NEGRA, true);

        editar.ejecutar(ADMIN, d.getUuidPublico(), comando(null, null, "Motivo corregido", "Banco Nacional de Bolivia"));

        assertEquals("Motivo corregido", d.getMotivo());
        assertEquals(1L, d.getIdEntidad());
        assertEquals(List.of(AccionAuditoria.DOMINIO_EDITADO), auditoria.acciones());
    }

    @Test
    void una_entidad_inexistente_es_rechazada() {
        assertThrows(IllegalArgumentException.class,
                () -> agregar.ejecutar(ADMIN, comando("x-falso.com", "negra", "m", "Banco Inventado")));
        assertTrue(dominios.filas.isEmpty());
    }

    @Test
    void un_formato_invalido_se_rechaza_en_el_dominio() {
        assertThrows(IllegalArgumentException.class,
                () -> agregar.ejecutar(ADMIN, comando("https://x-falso.com/login", "negra", "m", null)));
    }

    @Test
    void el_admin_de_empresa_no_gestiona_listas() {
        DominioEnLista d = dominios.agregarExistente("bnb-pagos.xyz", TipoLista.NEGRA, true);

        assertThrows(AccesoDenegadoException.class,
                () -> agregar.ejecutar(MARIA, comando("otro.xyz", "negra", "m", null)));
        assertThrows(AccesoDenegadoException.class,
                () -> cambiarEstado.ejecutar(MARIA, d.getUuidPublico(), false, IP));
        assertThrows(AccesoDenegadoException.class, () -> listar.entidades(MARIA));
        assertTrue(d.isActivo());
    }
}
