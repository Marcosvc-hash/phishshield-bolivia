package bo.phishshield.backend.analisis.domain.service;

import bo.phishshield.backend.analisis.domain.model.UrlAnalizada;

import java.util.Optional;

/** URL_SIN_HTTPS: cualquier sitio que pida credenciales sin cifrado es sospechoso. */
public class ReglaSinHttps implements ReglaUrl {

    @Override
    public String codigo() {
        return "URL_SIN_HTTPS";
    }

    @Override
    public Optional<String> evaluar(UrlAnalizada url) {
        if (!url.usaHttps()) {
            return Optional.of("La conexion no esta cifrada (esquema " + url.getEsquema() + ")");
        }
        return Optional.empty();
    }
}