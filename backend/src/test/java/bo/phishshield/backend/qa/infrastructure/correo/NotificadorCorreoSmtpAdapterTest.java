package bo.phishshield.backend.qa.infrastructure.correo;

import bo.phishshield.backend.qa.dobles.ServidorSmtpFalso;
import bo.phishshield.backend.qa.domain.model.CorreoDestino;
import bo.phishshield.backend.qa.domain.model.CorreoReporte;
import bo.phishshield.backend.qa.domain.model.EnvioCorreoException;
import bo.phishshield.backend.qa.infrastructure.config.CorreoPropiedades;
import bo.phishshield.backend.qa.infrastructure.reporte.CarpetaEvidencias;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** El adaptador SMTP manda de verdad el correo (con JavaMail) a un servidor SMTP falso local. */
class NotificadorCorreoSmtpAdapterTest {

    @TempDir
    Path carpeta;

    private ServidorSmtpFalso smtp;

    @BeforeEach
    void levantar() throws IOException {
        smtp = new ServidorSmtpFalso(0);
        Files.createDirectories(carpeta.resolve("e1"));
        Files.writeString(carpeta.resolve("e1/reporte-detallado.pdf"), "%PDF-1.4 falso");
        Files.writeString(carpeta.resolve("e1/reporte-resumen.html"), "<!doctype html><p>resumen</p>");
    }

    @AfterEach
    void bajar() throws IOException {
        smtp.close();
    }

    private NotificadorCorreoSmtpAdapter adaptador(String host, Integer puerto, String clave) {
        return new NotificadorCorreoSmtpAdapter(
                new CorreoPropiedades(host, puerto, "qa@phishshield.bo", clave, null, false),
                new CarpetaEvidencias(carpeta));
    }

    private static CorreoReporte correo() {
        return new CorreoReporte(new CorreoDestino("docente@ejemplo.bo"), "Reporte de automatización – Smoke",
                "PASS: 4   FAIL: 0", List.of(
                new CorreoReporte.Adjunto("reporte-detallado.pdf", "e1/reporte-detallado.pdf", "application/pdf"),
                new CorreoReporte.Adjunto("reporte-resumen.html", "e1/reporte-resumen.html", "text/html")));
    }

    @Test
    void sin_las_cuatro_variables_no_esta_configurado() {
        assertFalse(adaptador("", 25, "x").estaConfigurado());
        assertFalse(adaptador("localhost", null, "x").estaConfigurado());
        assertFalse(adaptador("localhost", 25, " ").estaConfigurado());
        assertTrue(adaptador("localhost", 25, "x").estaConfigurado());
    }

    @Test
    void envia_el_correo_con_los_dos_reportes_adjuntos() {
        adaptador("127.0.0.1", smtp.puerto(), "clave-de-prueba").enviar(correo());

        assertEquals(1, smtp.recibidos().size());
        ServidorSmtpFalso.Recibido r = smtp.recibidos().getFirst();
        assertEquals(List.of("docente@ejemplo.bo"), r.destinatarios());
        assertEquals("qa@phishshield.bo", r.usuario(), "Se autenticó con MAIL_USERNAME");
        assertTrue(r.datos().contains("Subject:"));
        assertTrue(r.datos().contains("filename=reporte-detallado.pdf") || r.datos().contains("filename=\"reporte-detallado.pdf\""));
        assertTrue(r.datos().contains("reporte-resumen.html"));
    }

    @Test
    void si_el_servidor_rechaza_la_cuenta_el_error_no_lleva_la_contrasena() {
        smtp.rechazarAutenticacion();

        EnvioCorreoException ex = assertThrows(EnvioCorreoException.class,
                () -> adaptador("127.0.0.1", smtp.puerto(), "clave-secreta-123").enviar(correo()));

        assertTrue(ex.getMessage().contains("docente@ejemplo.bo"));
        assertFalse(ex.getMessage().contains("clave-secreta-123"));
    }

    @Test
    void si_falta_un_adjunto_no_se_envia_nada() throws IOException {
        Files.delete(carpeta.resolve("e1/reporte-resumen.html"));

        assertThrows(EnvioCorreoException.class,
                () -> adaptador("127.0.0.1", smtp.puerto(), "clave").enviar(correo()));
        assertTrue(smtp.recibidos().isEmpty());
    }
}
