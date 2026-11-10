package bo.phishshield.backend.listas.infrastructure.web;

import bo.phishshield.backend.listas.application.AgregarDominioUseCase;
import bo.phishshield.backend.listas.application.CambiarEstadoDominioUseCase;
import bo.phishshield.backend.listas.application.ComandoDominio;
import bo.phishshield.backend.listas.application.EditarDominioUseCase;
import bo.phishshield.backend.listas.application.ListarDominiosUseCase;
import bo.phishshield.backend.listas.domain.model.EntidadSuplantada;
import bo.phishshield.backend.listas.domain.model.FiltroDominios;
import bo.phishshield.backend.listas.infrastructure.web.dto.DominioRequest;
import bo.phishshield.backend.listas.infrastructure.web.dto.DominioResponse;
import bo.phishshield.backend.shared.domain.model.SolicitudPagina;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;
import bo.phishshield.backend.shared.infrastructure.web.dto.CambiarEstadoRequest;
import bo.phishshield.backend.shared.infrastructure.web.dto.PaginaResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Listas blanca y negra (solo ADMIN: SeguridadConfig y, otra vez, los casos de uso).
 *
 *   GET    /api/admin/dominios?lista=&activo=&q=&pagina=
 *   POST   /api/admin/dominios                 agregar (o reactivar)
 *   PUT    /api/admin/dominios/{id}            corregir motivo y entidad
 *   PATCH  /api/admin/dominios/{id}/estado     {"activo": false} = "eliminar"
 *   GET    /api/admin/entidades                para el selector de entidad
 *
 * No hay DELETE a proposito: las filas nunca se borran.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminDominioController {

    private final ListarDominiosUseCase listarDominios;
    private final AgregarDominioUseCase agregarDominio;
    private final EditarDominioUseCase editarDominio;
    private final CambiarEstadoDominioUseCase cambiarEstado;

    public AdminDominioController(ListarDominiosUseCase listarDominios,
                                  AgregarDominioUseCase agregarDominio,
                                  EditarDominioUseCase editarDominio,
                                  CambiarEstadoDominioUseCase cambiarEstado) {
        this.listarDominios = listarDominios;
        this.agregarDominio = agregarDominio;
        this.editarDominio = editarDominio;
        this.cambiarEstado = cambiarEstado;
    }

    @GetMapping("/dominios")
    public PaginaResponse<DominioResponse> listar(
            @AuthenticationPrincipal UsuarioActual quien,
            @RequestParam(required = false) String lista,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "" + SolicitudPagina.TAMANO_POR_DEFECTO) int tamano) {

        return PaginaResponse.desde(
                listarDominios.ejecutar(quien, FiltroDominios.desdeTexto(lista, activo, q),
                        new SolicitudPagina(pagina, tamano)),
                DominioResponse::desde);
    }

    @PostMapping("/dominios")
    @ResponseStatus(HttpStatus.CREATED)
    public DominioResponse agregar(@AuthenticationPrincipal UsuarioActual quien,
                                   @Valid @RequestBody DominioRequest peticion,
                                   HttpServletRequest http) {
        return DominioResponse.desde(agregarDominio.ejecutar(quien, comando(peticion, http)));
    }

    @PutMapping("/dominios/{id}")
    public DominioResponse editar(@AuthenticationPrincipal UsuarioActual quien,
                                  @PathVariable UUID id,
                                  @Valid @RequestBody DominioRequest peticion,
                                  HttpServletRequest http) {
        return DominioResponse.desde(editarDominio.ejecutar(quien, id, comando(peticion, http)));
    }

    @PatchMapping("/dominios/{id}/estado")
    public DominioResponse cambiarEstado(@AuthenticationPrincipal UsuarioActual quien,
                                         @PathVariable UUID id,
                                         @Valid @RequestBody CambiarEstadoRequest peticion,
                                         HttpServletRequest http) {
        return DominioResponse.desde(cambiarEstado.ejecutar(quien, id, peticion.activo(), http.getRemoteAddr()));
    }

    @GetMapping("/entidades")
    public List<EntidadSuplantada> entidades(@AuthenticationPrincipal UsuarioActual quien) {
        return listarDominios.entidades(quien);
    }

    private static ComandoDominio comando(DominioRequest p, HttpServletRequest http) {
        return new ComandoDominio(p.dominio(), p.lista(), p.motivo(), p.entidad(), http.getRemoteAddr());
    }
}
