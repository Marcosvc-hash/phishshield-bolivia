package bo.phishshield.backend.qa.application;

import bo.phishshield.backend.qa.domain.model.CasoCatalogo;
import bo.phishshield.backend.qa.domain.port.out.CatalogoCasosPort;
import bo.phishshield.backend.qa.domain.port.out.EjecutorPruebasPort;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;

import java.util.List;
import java.util.Set;

/**
 * Caso de uso: el catalogo de casos activos, indicando cuales ya tienen
 * una prueba programada. Solo ADMIN.
 */
public class ListarCatalogoUseCase {

    private final CatalogoCasosPort catalogo;
    private final EjecutorPruebasPort ejecutor;

    public ListarCatalogoUseCase(CatalogoCasosPort catalogo, EjecutorPruebasPort ejecutor) {
        this.catalogo = catalogo;
        this.ejecutor = ejecutor;
    }

    public List<CasoCatalogo> ejecutar(UsuarioActual quien) {
        AlcanceDatos.soloPlataforma(quien);
        Set<String> implementados = ejecutor.codigosImplementados();
        return catalogo.listarActivos().stream()
                .map(caso -> new CasoCatalogo(caso, implementados.contains(caso.codigo())))
                .toList();
    }
}
