package bo.phishshield.backend.qa.domain.port.out;

import bo.phishshield.backend.qa.domain.model.ArchivoEvidencia;

import java.util.Optional;

/**
 * Lee archivos de la carpeta de evidencias (reportes y capturas). Las rutas
 * son relativas a esa carpeta; una ruta que intente salir de ella
 * ("../...") se trata como inexistente.
 */
public interface EvidenciasPort {

    Optional<ArchivoEvidencia> leer(String rutaRelativa);
}
