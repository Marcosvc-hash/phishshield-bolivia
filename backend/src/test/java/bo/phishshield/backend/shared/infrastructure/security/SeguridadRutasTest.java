package bo.phishshield.backend.shared.infrastructure.security;

import bo.phishshield.backend.shared.domain.model.Rol;
import bo.phishshield.backend.usuario.domain.model.EstadoUsuario;
import bo.phishshield.backend.usuario.domain.model.Usuario;
import bo.phishshield.backend.usuario.domain.port.out.UsuarioRepositoryPort;
import bo.phishshield.backend.usuario.infrastructure.security.JwtTokenAccesoAdapter;
import bo.phishshield.backend.usuario.infrastructure.web.UsuarioController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba las reglas de SeguridadConfig con tokens JWT firmados de verdad:
 * pasan por la validacion de firma y por JwtUsuarioActualConverter.
 * La base se reemplaza por un repositorio simulado.
 */
@WebMvcTest(UsuarioController.class)
@Import({SeguridadConfig.class, RespuestasSeguridad.class, JwtTokenAccesoAdapter.class})
class SeguridadRutasTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokenAccesoAdapter tokens;

    @MockitoBean
    private UsuarioRepositoryPort usuarioRepository;

    private String tokenDe(Rol rol) {
        Usuario usuario = new Usuario(7L, UUID.randomUUID(), null, "Prueba", null,
                "prueba@phishshield.bo", "hash", rol, EstadoUsuario.ACTIVO, true, 0, null, null);
        when(usuarioRepository.buscarPorUuid(usuario.getUuidPublico())).thenReturn(Optional.of(usuario));
        return tokens.emitir(usuario).valor();
    }

    @Test
    void sin_token_una_ruta_protegida_devuelve_401_en_json() throws Exception {
        mvc.perform(get("/api/usuarios"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401));
    }

    @Test
    void un_token_alterado_devuelve_401() throws Exception {
        String alterado = tokenDe(Rol.ADMIN) + "x";

        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + alterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void con_rol_usuario_una_ruta_de_admin_devuelve_403() throws Exception {
        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + tokenDe(Rol.USUARIO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403));
    }

    @Test
    void con_rol_admin_la_ruta_responde_200() throws Exception {
        when(usuarioRepository.listarTodos()).thenReturn(List.of());

        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + tokenDe(Rol.ADMIN)))
                .andExpect(status().isOk());
    }
}
