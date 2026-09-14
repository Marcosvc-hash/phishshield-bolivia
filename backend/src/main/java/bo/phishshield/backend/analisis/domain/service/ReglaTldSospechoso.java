package bo.phishshield.backend.analisis.domain.service;

import bo.phishshield.backend.analisis.domain.model.UrlAnalizada;

import java.util.Optional;
import java.util.Set;

/** URL_TLD_SOSPECHOSO: extensiones baratas o gratuitas, muy usadas en campañas de phishing. */
public class ReglaTldSospechoso implements ReglaUrl {

    private static final Set<String> TLDS_ABUSADOS = Set.of(
            ".xyz", ".top", ".info", ".tk", ".ml", ".ga", ".cf", ".gq",
            ".club", ".online", ".site", ".click", ".link", ".work", ".loan"
    );

    @Override
    public String codigo() {
        return "URL_TLD_SOSPECHOSO";
    }

    @Override
    public Optional<String> evaluar(UrlAnalizada url) {
        String tld = url.getTld();
        if (TLDS_ABUSADOS.contains(tld)) {
            return Optional.of("Extension de dominio asociada a abuso: " + tld);
        }
        return Optional.empty();
    }
}