package bo.phishshield.backend.qa.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Asistente de IA del Centro de Pruebas (bloque phishshield.qa.asistente).
 * Solo se activa si existe ANTHROPIC_API_KEY; sin ella la pantalla oculta
 * sus funciones. La clave nunca se escribe en el log.
 *
 * @param modelo QA_IA_MODELO (por defecto claude-opus-5-5)
 * @param url    QA_IA_API_URL (por defecto https://api.anthropic.com; se cambia en las pruebas)
 */
@ConfigurationProperties("phishshield.qa.asistente")
public record AsistentePropiedades(String clave, String modelo, String url) {

    public boolean configurado() {
        return clave != null && !clave.isBlank();
    }

    public String modeloEfectivo() {
        return modelo == null || modelo.isBlank() ? "claude-opus-5-5" : modelo.strip();
    }

    public String urlEfectiva() {
        return url == null || url.isBlank() ? "https://api.anthropic.com" : url.strip().replaceAll("/+$", "");
    }

    @Override
    public String toString() {
        return "AsistentePropiedades[clave=" + (configurado() ? "***" : "(vacia)") + ", modelo=" + modelo
                + ", url=" + url + "]";
    }
}
