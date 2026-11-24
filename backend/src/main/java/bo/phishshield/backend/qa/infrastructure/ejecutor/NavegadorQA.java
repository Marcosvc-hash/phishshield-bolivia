package bo.phishshield.backend.qa.infrastructure.ejecutor;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * La pestana de Chromium de un caso de interfaz. Cada caso tiene su propio
 * contexto de navegador (cookies y localStorage aislados, como una ventana
 * de incognito) y se cierra al terminar el caso.
 *
 * Las capturas se guardan en qa-evidencias/{ejecucion}/capturas/ con el
 * codigo del caso y un numero correlativo.
 */
public final class NavegadorQA implements AutoCloseable {

    /** Lo que espera una accion antes de rendirse (por ejemplo, que aparezca un elemento). */
    static final double ESPERA_MS = 15_000;

    private final BrowserContext contexto;
    private final Page pagina;
    private final Path carpetaEvidencias;
    private final Path carpetaCapturas;
    private final String codigo;
    private int capturas;

    NavegadorQA(Browser navegador, Path carpetaEvidencias, Path carpetaEjecucion, String codigo) {
        this.contexto = navegador.newContext(new Browser.NewContextOptions()
                .setViewportSize(1366, 860)
                .setLocale("es-BO")
                .setTimezoneId("America/La_Paz"));
        this.contexto.setDefaultTimeout(ESPERA_MS);
        this.pagina = contexto.newPage();
        this.carpetaEvidencias = carpetaEvidencias;
        this.carpetaCapturas = carpetaEjecucion.resolve("capturas");
        this.codigo = codigo;
    }

    public Page pagina() {
        return pagina;
    }

    /**
     * Abre una URL. Si el frontend no responde (conexion rechazada, DNS), es
     * un fallo de infraestructura: no dice nada de la pantalla.
     */
    public void ir(String url) {
        try {
            pagina.navigate(url);
        } catch (PlaywrightException ex) {
            if (String.valueOf(ex.getMessage()).contains("net::ERR_")) {
                throw new FalloInfraestructuraException("El frontend no responde en " + url
                        + " (¿está levantado con $env:PORT = \"4200\"?)", ex);
            }
            throw ex;
        }
    }

    /** Guarda una captura de toda la pagina y devuelve su ruta relativa a la carpeta de evidencias. */
    String capturar(String descripcion) {
        try {
            Files.createDirectories(carpetaCapturas);
            capturas++;
            Path archivo = carpetaCapturas.resolve(String.format(Locale.ROOT, "%s-%02d-%s.png",
                    codigo, capturas, nombreArchivo(descripcion)));
            pagina.screenshot(new Page.ScreenshotOptions().setPath(archivo).setFullPage(true));
            return carpetaEvidencias.relativize(archivo).toString().replace('\\', '/');
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /** "Pulsar Analizar con el campo vacío" -> "pulsar-analizar-con-el-campo-vacio" (corto y sin tildes). */
    static String nombreArchivo(String descripcion) {
        String sinTildes = java.text.Normalizer.normalize(descripcion, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String limpio = sinTildes.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return limpio.length() > 40 ? limpio.substring(0, 40).replaceAll("-$", "") : limpio;
    }

    @Override
    public void close() {
        try {
            contexto.close();
        } catch (RuntimeException ignorada) {
            // El navegador ya pudo haberse cerrado; no hay nada mas que hacer
        }
    }
}
