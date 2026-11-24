package bo.phishshield.backend.qa.application;

import bo.phishshield.backend.qa.domain.model.ArchivoEvidencia;
import bo.phishshield.backend.qa.domain.model.EvidenciaNoEncontradaException;
import bo.phishshield.backend.qa.domain.port.out.EvidenciasPort;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;

import java.util.Locale;
import java.util.UUID;

/**
 * Caso de uso: una captura de pantalla de un paso, para verla en el Centro
 * de Pruebas. Solo se entregan imagenes PNG de la carpeta de capturas de
 * ESA ejecucion ({id}/capturas/...): ni reportes, ni archivos de otra
 * ejecucion, ni rutas con "..". Solo ADMIN.
 */
public class ObtenerEvidenciaUseCase {

    private final EvidenciasPort evidencias;

    public ObtenerEvidenciaUseCase(EvidenciasPort evidencias) {
        this.evidencias = evidencias;
    }

    public ArchivoEvidencia ejecutar(UsuarioActual quien, UUID idEjecucion, String ruta) {
        AlcanceDatos.soloPlataforma(quien);
        String limpia = ruta == null ? "" : ruta.strip().replace('\\', '/');
        boolean esCapturaDeLaEjecucion = limpia.startsWith(idEjecucion + "/capturas/")
                && limpia.toLowerCase(Locale.ROOT).endsWith(".png")
                && !limpia.contains("..");
        if (!esCapturaDeLaEjecucion) {
            throw new EvidenciaNoEncontradaException(limpia);
        }
        return evidencias.leer(limpia).orElseThrow(() -> new EvidenciaNoEncontradaException(limpia));
    }
}
