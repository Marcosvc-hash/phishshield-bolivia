package bo.phishshield.backend.qa.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion del Centro de Pruebas (bloque phishshield.qa del
 * application.yaml). Los valores salen de variables de entorno:
 *
 *   QA_BASE_URL       backend que se prueba, por ejemplo http://localhost:8080
 *   QA_FRONTEND_URL   frontend que se prueba, por ejemplo http://localhost:4200
 *   QA_TEST_USER      correo del administrador de prueba
 *   QA_TEST_PASSWORD  su contrasena (nunca se escribe en la evidencia ni en el log)
 *   QA_EVIDENCIAS_DIR carpeta de evidencias y reportes (ignorada por git)
 *   QA_AMBIENTE       nombre del ambiente que sale en los reportes
 *   QA_PERMITIR_BASE_REAL  true solo si de verdad se quiere probar contra la base real
 *
 * Es un record: equivale a una clase de C# con propiedades init-only.
 */
@ConfigurationProperties("phishshield.qa")
public record QaPropiedades(
        String baseUrl,
        String frontendUrl,
        String usuario,
        String contrasena,
        String carpetaEvidencias,
        String ambiente,
        boolean permitirBaseReal
) {

    /** Sin la contrasena: un record la imprimiria en el toString() por defecto. */
    @Override
    public String toString() {
        return "QaPropiedades[baseUrl=" + baseUrl + ", frontendUrl=" + frontendUrl + ", usuario=" + usuario
                + ", contrasena=" + (contrasena == null || contrasena.isBlank() ? "(vacia)" : "***")
                + ", carpetaEvidencias=" + carpetaEvidencias + ", ambiente=" + ambiente
                + ", permitirBaseReal=" + permitirBaseReal + "]";
    }
}
