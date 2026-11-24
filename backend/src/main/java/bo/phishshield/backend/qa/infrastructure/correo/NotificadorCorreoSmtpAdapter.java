package bo.phishshield.backend.qa.infrastructure.correo;

import bo.phishshield.backend.qa.domain.model.CorreoReporte;
import bo.phishshield.backend.qa.domain.model.EnvioCorreoException;
import bo.phishshield.backend.qa.domain.port.out.NotificadorCorreoPort;
import bo.phishshield.backend.qa.infrastructure.config.CorreoPropiedades;
import bo.phishshield.backend.qa.infrastructure.reporte.CarpetaEvidencias;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Envia los reportes por SMTP. El remitente se arma aqui con las variables
 * MAIL_* (no se usa la configuracion automatica de Spring, spring.mail.*,
 * para que sin variables no exista ningun cliente de correo).
 *
 * Los mensajes de error que se guardan nunca llevan la contrasena: solo el
 * tipo de falla y la respuesta del servidor.
 */
public class NotificadorCorreoSmtpAdapter implements NotificadorCorreoPort {

    private final CorreoPropiedades propiedades;
    private final CarpetaEvidencias carpeta;

    public NotificadorCorreoSmtpAdapter(CorreoPropiedades propiedades, CarpetaEvidencias carpeta) {
        this.propiedades = propiedades;
        this.carpeta = carpeta;
    }

    @Override
    public boolean estaConfigurado() {
        return propiedades.completa();
    }

    @Override
    public void enviar(CorreoReporte correo) {
        if (!estaConfigurado()) {
            throw new EnvioCorreoException("No hay servidor SMTP configurado", null);
        }
        JavaMailSenderImpl remitente = remitente();
        try {
            MimeMessage mensaje = remitente.createMimeMessage();
            MimeMessageHelper ayuda = new MimeMessageHelper(mensaje, true, StandardCharsets.UTF_8.name());
            ayuda.setFrom(propiedades.remitenteEfectivo());
            ayuda.setTo(correo.destino().valor());
            ayuda.setSubject(correo.asunto());
            ayuda.setText(correo.cuerpo(), false);
            for (CorreoReporte.Adjunto adjunto : correo.adjuntos()) {
                Path archivo = carpeta.resolver(adjunto.ruta()).filter(Files::isRegularFile)
                        .orElseThrow(() -> new EnvioCorreoException("Falta el adjunto " + adjunto.nombreArchivo(), null));
                ayuda.addAttachment(adjunto.nombreArchivo(), new FileSystemResource(archivo), adjunto.tipoMime());
            }
            remitente.send(mensaje);
        } catch (MailException | MessagingException ex) {
            throw new EnvioCorreoException("No se pudo enviar el correo a " + correo.destino().valor() + " ("
                    + ex.getClass().getSimpleName() + ": " + causaRaiz(ex) + ")", ex);
        }
    }

    private JavaMailSenderImpl remitente() {
        JavaMailSenderImpl remitente = new JavaMailSenderImpl();
        remitente.setHost(propiedades.host().strip());
        remitente.setPort(propiedades.puerto());
        remitente.setUsername(propiedades.usuario().strip());
        remitente.setPassword(propiedades.contrasena());
        remitente.setDefaultEncoding(StandardCharsets.UTF_8.name());
        Properties extra = remitente.getJavaMailProperties();
        extra.put("mail.smtp.auth", "true");
        extra.put("mail.smtp.starttls.enable", String.valueOf(propiedades.usarStarttls()));
        extra.put("mail.smtp.connectiontimeout", "10000");
        extra.put("mail.smtp.timeout", "20000");
        extra.put("mail.smtp.writetimeout", "20000");
        return remitente;
    }

    /** El mensaje mas especifico de la cadena, sin datos de la cuenta. */
    static String causaRaiz(Throwable ex) {
        Throwable t = ex;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        String mensaje = t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
        return mensaje.replaceAll("\\s+", " ").strip();
    }
}
