package bo.phishshield.backend.analisis.domain.service;

import bo.phishshield.backend.analisis.domain.model.UrlAnalizada;

import java.util.Optional;

/** URL_IP_LITERAL: un banco no publica su portal en una IP desnuda. */
public class ReglaIpLiteral implements ReglaUrl {

    @Override
    public String codigo() {
        return "URL_IP_LITERAL";
    }

    @Override
    public Optional<String> evaluar(UrlAnalizada url) {
        if (url.hostEsIpLiteral()) {
            return Optional.of("La direccion usa la IP " + url.getHost() + " en lugar de un dominio");
        }
        return Optional.empty();
    }
}