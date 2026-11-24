package bo.phishshield.backend.qa.infrastructure.reporte;

import bo.phishshield.backend.qa.domain.model.ArchivoEvidencia;
import bo.phishshield.backend.qa.domain.port.out.EvidenciasPort;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

/** Lee reportes y capturas de la carpeta de evidencias en disco. */
public class EvidenciasArchivoAdapter implements EvidenciasPort {

    private final CarpetaEvidencias carpeta;

    public EvidenciasArchivoAdapter(CarpetaEvidencias carpeta) {
        this.carpeta = carpeta;
    }

    @Override
    public Optional<ArchivoEvidencia> leer(String rutaRelativa) {
        Optional<Path> ruta = carpeta.resolver(rutaRelativa).filter(Files::isRegularFile);
        if (ruta.isEmpty()) {
            return Optional.empty();
        }
        try {
            Path archivo = ruta.get();
            return Optional.of(new ArchivoEvidencia(archivo.getFileName().toString(),
                    tipoMime(archivo.getFileName().toString()), Files.readAllBytes(archivo)));
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo leer la evidencia " + rutaRelativa, ex);
        }
    }

    static String tipoMime(String nombre) {
        String n = nombre.toLowerCase(Locale.ROOT);
        if (n.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (n.endsWith(".html")) {
            return "text/html; charset=UTF-8";
        }
        if (n.endsWith(".png")) {
            return "image/png";
        }
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        return "application/octet-stream";
    }
}
