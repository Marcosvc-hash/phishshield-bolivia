package bo.phishshield.backend.analisis.domain.service;

import bo.phishshield.backend.analisis.domain.model.UrlAnalizada;

import java.util.Optional;

/**
 * URL_SUBDOMINIOS: tecnica clasica de ocultamiento.
 * "bnb.com.bo.verificacion.sitio-falso.xyz" parece del BNB a simple vista,
 * pero el dominio real es sitio-falso.xyz.
 */
public class ReglaSubdominios implements ReglaUrl {

    private static final int UMBRAL = 3;

    @Override
    public String codigo() {
        return "URL_SUBDOMINIOS";
    }

    @Override
    public Optional<String> evaluar(UrlAnalizada url) {
        int cantidad = url.getCantidadSubdominios();
        if (cantidad >= UMBRAL) {
            return Optional.of("Cantidad anomala de subdominios: " + cantidad);
        }
        return Optional.empty();
    }
}