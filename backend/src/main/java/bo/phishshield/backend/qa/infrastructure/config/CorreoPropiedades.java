package bo.phishshield.backend.qa.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Servidor SMTP para enviar los reportes (bloque phishshield.qa.correo).
 * Todo sale de variables de entorno; si falta MAIL_HOST, MAIL_PORT,
 * MAIL_USERNAME o MAIL_PASSWORD, el envio se omite (estado no_configurado).
 *
 * @param remitente MAIL_FROM; si falta, se usa MAIL_USERNAME
 * @param starttls  MAIL_STARTTLS (por defecto true: Gmail, Outlook y la mayoria lo piden en el puerto 587)
 */
@ConfigurationProperties("phishshield.qa.correo")
public record CorreoPropiedades(
        String host,
        Integer puerto,
        String usuario,
        String contrasena,
        String remitente,
        Boolean starttls
) {

    public boolean completa() {
        return lleno(host) && puerto != null && puerto > 0 && lleno(usuario) && lleno(contrasena);
    }

    public String remitenteEfectivo() {
        return lleno(remitente) ? remitente.strip() : usuario;
    }

    public boolean usarStarttls() {
        return starttls == null || starttls;
    }

    private static boolean lleno(String texto) {
        return texto != null && !texto.isBlank();
    }

    /** Sin la contrasena: un record la imprimiria en el toString() por defecto. */
    @Override
    public String toString() {
        return "CorreoPropiedades[host=" + host + ", puerto=" + puerto + ", usuario=" + usuario
                + ", contrasena=" + (lleno(contrasena) ? "***" : "(vacia)") + ", remitente=" + remitente
                + ", starttls=" + starttls + "]";
    }
}
