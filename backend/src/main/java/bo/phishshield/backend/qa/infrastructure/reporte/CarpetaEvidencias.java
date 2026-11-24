package bo.phishshield.backend.qa.infrastructure.reporte;

import bo.phishshield.backend.qa.infrastructure.config.QaPropiedades;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

/**
 * La carpeta donde se guardan las evidencias y los reportes
 * (QA_EVIDENCIAS_DIR, por defecto backend/qa-evidencias, ignorada por git).
 * Dentro hay una subcarpeta por ejecucion: qa-evidencias/{uuid}/...
 */
public final class CarpetaEvidencias {

    public static final String POR_DEFECTO = "qa-evidencias";

    private final Path base;

    public CarpetaEvidencias(Path base) {
        this.base = base.toAbsolutePath().normalize();
    }

    public static CarpetaEvidencias desde(QaPropiedades propiedades) {
        String carpeta = propiedades.carpetaEvidencias();
        return new CarpetaEvidencias(Path.of(carpeta == null || carpeta.isBlank() ? POR_DEFECTO : carpeta));
    }

    public Path base() {
        return base;
    }

    public Path deEjecucion(UUID id) {
        return base.resolve(id.toString());
    }

    /** Ruta relativa con barras normales ("{uuid}/reporte-detallado.pdf"), como se guarda en la base. */
    public String relativa(Path archivo) {
        return base.relativize(archivo.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    /** Resuelve una ruta relativa; vacio si intenta salir de la carpeta ("../../etc"). */
    public Optional<Path> resolver(String rutaRelativa) {
        if (rutaRelativa == null || rutaRelativa.isBlank()) {
            return Optional.empty();
        }
        Path ruta;
        try {
            ruta = base.resolve(rutaRelativa.replace('\\', '/')).normalize();
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
        return ruta.startsWith(base) && !ruta.equals(base) ? Optional.of(ruta) : Optional.empty();
    }
}
