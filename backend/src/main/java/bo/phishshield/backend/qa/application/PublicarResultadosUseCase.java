package bo.phishshield.backend.qa.application;

import bo.phishshield.backend.qa.domain.model.ArchivosReporte;
import bo.phishshield.backend.qa.domain.model.CorreoReporte;
import bo.phishshield.backend.qa.domain.model.Ejecucion;
import bo.phishshield.backend.qa.domain.model.EnvioCorreoException;
import bo.phishshield.backend.qa.domain.model.FormatoReporte;
import bo.phishshield.backend.qa.domain.model.ResultadoCaso;
import bo.phishshield.backend.qa.domain.model.ResumenEjecucion;
import bo.phishshield.backend.qa.domain.port.out.EjecucionRepositoryPort;
import bo.phishshield.backend.qa.domain.port.out.GeneradorReportePort;
import bo.phishshield.backend.qa.domain.port.out.NotificadorCorreoPort;

import java.lang.System.Logger.Level;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Caso de uso: lo que se hace cuando una ejecucion termina.
 *
 *  1. Genera el PDF detallado y el HTML resumido y guarda sus rutas.
 *  2. Si hay SMTP configurado (MAIL_*), los envia al destinatario elegido;
 *     si no, deja el correo en no_configurado. Un error de envio queda
 *     registrado en la ejecucion.
 *
 * Nunca lanza excepciones: si algo falla, el resultado de las pruebas no
 * cambia y los reportes se pueden descargar desde la pantalla.
 */
public class PublicarResultadosUseCase implements TrasEjecucion {

    private static final System.Logger log = System.getLogger(PublicarResultadosUseCase.class.getName());
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final EjecucionRepositoryPort ejecuciones;
    private final GeneradorReportePort generador;
    private final NotificadorCorreoPort notificador;
    private final Clock reloj;
    private final ZoneId zona;

    public PublicarResultadosUseCase(EjecucionRepositoryPort ejecuciones, GeneradorReportePort generador,
                                     NotificadorCorreoPort notificador, Clock reloj, ZoneId zona) {
        this.ejecuciones = ejecuciones;
        this.generador = generador;
        this.notificador = notificador;
        this.reloj = reloj;
        this.zona = zona;
    }

    @Override
    public void alTerminar(Ejecucion ejecucion) {
        try {
            ArchivosReporte archivos = generador.generar(ejecucion);
            ejecucion.registrarReportes(archivos);
        } catch (RuntimeException ex) {
            log.log(Level.WARNING, "No se pudieron generar los reportes de la ejecucion "
                    + ejecucion.getUuidPublico() + ": " + ex.getMessage(), ex);
        }

        try {
            enviarCorreo(ejecucion);
        } catch (RuntimeException ex) {
            log.log(Level.WARNING, "Error inesperado al enviar el correo de la ejecucion "
                    + ejecucion.getUuidPublico() + ": " + ex.getMessage(), ex);
        }

        try {
            ejecuciones.guardar(ejecucion);
        } catch (RuntimeException ex) {
            log.log(Level.WARNING, "No se pudo guardar el resultado de los reportes y el correo de la ejecucion "
                    + ejecucion.getUuidPublico() + ": " + ex.getMessage(), ex);
        }
    }

    private void enviarCorreo(Ejecucion ejecucion) {
        if (!notificador.estaConfigurado()) {
            ejecucion.marcarCorreoNoConfigurado();
            return;
        }
        if (!ejecucion.tieneReportes()) {
            ejecucion.marcarErrorCorreo("No se enviaron los reportes porque no se pudieron generar");
            return;
        }
        try {
            notificador.enviar(correo(ejecucion));
            ejecucion.marcarCorreoEnviado(OffsetDateTime.now(reloj));
        } catch (EnvioCorreoException ex) {
            ejecucion.marcarErrorCorreo(ex.getMessage());
        }
    }

    CorreoReporte correo(Ejecucion e) {
        ResumenEjecucion r = e.resumen();
        StringBuilder cuerpo = new StringBuilder();
        cuerpo.append("Centro de Pruebas QA - PhishShield Bolivia\n\n")
                .append("Suite: ").append(e.getSuite().etiqueta()).append(" (").append(e.tipo().etiqueta()).append(")\n")
                .append("Ambiente: ").append(e.getAmbiente()).append('\n')
                .append("Inicio: ").append(fecha(e.getInicio())).append('\n')
                .append("Fin: ").append(fecha(e.getFin())).append('\n')
                .append("Resultado global: ")
                .append(e.getResultadoGlobal() != null ? e.getResultadoGlobal().etiqueta() : "sin resultado (interrumpida)")
                .append("\n\n")
                .append("PASS: ").append(r.pass()).append("   FAIL: ").append(r.fail())
                .append("   SKIPPED: ").append(r.skipped()).append("   NOT_RUN: ").append(r.notRun())
                .append("   (").append(r.porcentajeExito()).append(" % de éxito)\n");
        List<ResultadoCaso> fallidos = e.getResultados().stream()
                .filter(c -> c.getTipoFallo() != null)
                .toList();
        if (!fallidos.isEmpty()) {
            cuerpo.append("\nCasos con FAIL:\n");
            fallidos.forEach(c -> cuerpo.append(" - ").append(c.getCodigo()).append(": ")
                    .append(c.observacionResumida()).append('\n'));
        }
        cuerpo.append("\nSe adjuntan el reporte detallado (PDF) y el resumen (HTML).\n");

        return new CorreoReporte(e.getCorreo(), e.asuntoCorreo(zona), cuerpo.toString(), List.of(
                new CorreoReporte.Adjunto(FormatoReporte.PDF.nombreArchivo(), e.getRutaReportePdf(), "application/pdf"),
                new CorreoReporte.Adjunto(FormatoReporte.HTML.nombreArchivo(), e.getRutaReporteHtml(), "text/html")));
    }

    private String fecha(OffsetDateTime fecha) {
        return fecha == null ? "-" : fecha.atZoneSameInstant(zona).format(FECHA);
    }
}
