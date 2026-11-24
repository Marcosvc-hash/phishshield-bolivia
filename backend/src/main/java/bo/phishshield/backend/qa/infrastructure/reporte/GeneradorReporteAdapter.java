package bo.phishshield.backend.qa.infrastructure.reporte;

import bo.phishshield.backend.qa.domain.model.ArchivosReporte;
import bo.phishshield.backend.qa.domain.model.Ejecucion;
import bo.phishshield.backend.qa.domain.model.FormatoReporte;
import bo.phishshield.backend.qa.domain.port.out.GeneradorReportePort;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.OffsetDateTime;

/**
 * Escribe los dos reportes en la carpeta de la ejecucion:
 *   qa-evidencias/{uuid}/reporte-detallado.pdf
 *   qa-evidencias/{uuid}/reporte-resumen.html
 * Se escriben primero en un archivo temporal y luego se renombran, para
 * que nadie descargue un reporte a medio escribir.
 */
public class GeneradorReporteAdapter implements GeneradorReportePort {

    private final CarpetaEvidencias carpeta;
    private final Clock reloj;

    public GeneradorReporteAdapter(CarpetaEvidencias carpeta, Clock reloj) {
        this.carpeta = carpeta;
        this.reloj = reloj;
    }

    @Override
    public ArchivosReporte generar(Ejecucion ejecucion) {
        OffsetDateTime ahora = OffsetDateTime.now(reloj);
        Path destino = carpeta.deEjecucion(ejecucion.getUuidPublico());
        Path pdf = destino.resolve(FormatoReporte.PDF.nombreArchivo());
        Path html = destino.resolve(FormatoReporte.HTML.nombreArchivo());
        try {
            Files.createDirectories(destino);
            escribir(pdf, new ReportePdf(carpeta).generar(ejecucion, ahora));
            escribir(html, ReporteHtml.generar(ejecucion, ahora).getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudieron escribir los reportes en " + destino, ex);
        }
        return new ArchivosReporte(carpeta.relativa(pdf), carpeta.relativa(html));
    }

    private static void escribir(Path archivo, byte[] contenido) throws IOException {
        Path temporal = archivo.resolveSibling(archivo.getFileName() + ".tmp");
        Files.write(temporal, contenido);
        Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
