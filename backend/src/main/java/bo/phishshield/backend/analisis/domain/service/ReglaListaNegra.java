package bo.phishshield.backend.analisis.domain.service;

import bo.phishshield.backend.analisis.domain.model.DominioListado;
import bo.phishshield.backend.analisis.domain.model.UrlAnalizada;

import java.util.List;
import java.util.Optional;

/**
 * URL_LISTA_NEGRA: el host es un dominio ya reportado, o un subdominio suyo.
 * Se compara por etiquetas completas: "login.bnb-bo-seguro.com" coincide con
 * "bnb-bo-seguro.com", pero "otrobnb-bo-seguro.com" no.
 */
public class ReglaListaNegra implements ReglaUrl {

    private final List<DominioListado> listaNegra;

    public ReglaListaNegra(List<DominioListado> listaNegra) {
        this.listaNegra = List.copyOf(listaNegra);
    }

    @Override
    public String codigo() {
        return "URL_LISTA_NEGRA";
    }

    @Override
    public Optional<String> evaluar(UrlAnalizada url) {
        String host = url.getHost();

        for (DominioListado listado : listaNegra) {
            String dominio = listado.dominio();
            if (host.equals(dominio) || host.endsWith("." + dominio)) {
                return Optional.of(evidencia(listado));
            }
        }
        return Optional.empty();
    }

    private String evidencia(DominioListado listado) {
        String texto = listado.dominio() + " figura en la lista negra de PhishShield";
        if (listado.motivo() != null && !listado.motivo().isBlank()) {
            texto += ": " + listado.motivo();
        }
        return texto;
    }
}
