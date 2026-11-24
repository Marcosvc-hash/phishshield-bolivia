package bo.phishshield.backend.qa.infrastructure.ejecutor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Lo que los casos necesitan saber del entorno: a donde apuntar y con que
 * usuario. Cada caso pide solo lo que usa (ver ContextoCaso.requiere...).
 */
public record EntornoQA(
        String baseUrl,
        String frontendUrl,
        String usuario,
        String contrasena,
        String iaUrl,
        Path carpetaEvidencias
) {

    public EntornoQA {
        baseUrl = sinBarraFinal(baseUrl);
        frontendUrl = sinBarraFinal(frontendUrl);
        iaUrl = sinBarraFinal(iaUrl);
    }

    /** Variables que faltan para un caso de API con sesion. */
    public List<String> faltantes(boolean necesitaSesion) {
        List<String> faltan = new ArrayList<>();
        if (vacio(baseUrl)) {
            faltan.add("QA_BASE_URL");
        }
        if (necesitaSesion && vacio(usuario)) {
            faltan.add("QA_TEST_USER");
        }
        if (necesitaSesion && vacio(contrasena)) {
            faltan.add("QA_TEST_PASSWORD");
        }
        return faltan;
    }

    public static boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }

    private static String sinBarraFinal(String url) {
        if (url == null) {
            return null;
        }
        String limpia = url.strip();
        return limpia.endsWith("/") ? limpia.substring(0, limpia.length() - 1) : limpia;
    }

    @Override
    public String toString() {
        return "EntornoQA[baseUrl=" + baseUrl + ", frontendUrl=" + frontendUrl + ", usuario=" + usuario
                + ", contrasena=" + (vacio(contrasena) ? "(vacia)" : "***") + ", iaUrl=" + iaUrl
                + ", carpetaEvidencias=" + carpetaEvidencias + "]";
    }
}
