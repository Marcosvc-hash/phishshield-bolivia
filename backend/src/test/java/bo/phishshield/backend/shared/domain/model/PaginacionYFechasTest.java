package bo.phishshield.backend.shared.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PaginacionYFechasTest {

    @Test
    void la_solicitud_corrige_valores_fuera_de_rango() {
        assertEquals(new SolicitudPagina(0, 20), new SolicitudPagina(-3, 0));
        assertEquals(SolicitudPagina.TAMANO_MAXIMO, new SolicitudPagina(0, 5000).tamano());
        assertEquals(40, new SolicitudPagina(2, 20).desplazamiento());
    }

    @Test
    void la_pagina_calcula_el_total_de_paginas_redondeando_hacia_arriba() {
        Pagina<String> pagina = Pagina.de(List.of("a", "b"), new SolicitudPagina(0, 20), 41);
        assertEquals(3, pagina.totalPaginas());
        assertEquals(0, Pagina.de(List.of(), new SolicitudPagina(0, 20), 0).totalPaginas());
    }

    @Test
    void el_rango_incluye_el_dia_hasta_completo_en_hora_de_bolivia() {
        RangoFechas rango = new RangoFechas(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 9));

        assertEquals(OffsetDateTime.parse("2026-11-01T00:00-04:00"), rango.inicio());
        assertEquals(OffsetDateTime.parse("2026-11-10T00:00-04:00"), rango.finExclusivo());
    }

    @Test
    void el_rango_sin_extremos_no_limita() {
        assertNull(RangoFechas.sinLimites().inicio());
        assertNull(RangoFechas.sinLimites().finExclusivo());
    }

    @Test
    void desde_posterior_a_hasta_es_rechazado() {
        assertThrows(IllegalArgumentException.class,
                () -> new RangoFechas(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 1)));
    }

    @Test
    void el_alcance_de_empresa_no_incluye_datos_de_otra_ni_anonimos() {
        AlcanceDatos deEmpresa = AlcanceDatos.administrativo(
                new UsuarioActual(1L, UUID.randomUUID(), "m@x.bo", 5L, Rol.ADMIN_EMPRESA));

        assertTrue(deEmpresa.incluye(5L));
        assertFalse(deEmpresa.incluye(6L));
        assertFalse(deEmpresa.incluye(null));
        assertTrue(new AlcanceDatos(null).incluye(null));
    }

    @Test
    void solo_el_admin_entra_a_las_secciones_de_plataforma() {
        UsuarioActual admin = new UsuarioActual(1L, UUID.randomUUID(), "a@x.bo", null, Rol.ADMIN);
        UsuarioActual deEmpresa = new UsuarioActual(2L, UUID.randomUUID(), "m@x.bo", 5L, Rol.ADMIN_EMPRESA);

        assertTrue(AlcanceDatos.soloPlataforma(admin).esGlobal());
        assertThrows(AccesoDenegadoException.class, () -> AlcanceDatos.soloPlataforma(deEmpresa));
    }
}
