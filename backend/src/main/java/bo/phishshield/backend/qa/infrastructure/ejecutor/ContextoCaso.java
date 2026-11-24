package bo.phishshield.backend.qa.infrastructure.ejecutor;

import bo.phishshield.backend.qa.domain.model.MetricaRendimiento;
import bo.phishshield.backend.qa.domain.model.UrlEtiquetada;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Lo que recibe cada metodo de prueba como parametro: el entorno, el
 * cliente de API, los parametros de la ejecucion y la bitacora de pasos.
 *
 * Uso tipico dentro de un caso:
 * <pre>
 *   Response r = ctx.paso("Enviar POST /api/analisis", () -> ctx.api().sinSesion().body(...).post(...));
 *   ctx.paso("Verificar el resultado", () -> assertEquals("seguro", r.jsonPath().getString("resultado")));
 * </pre>
 */
public final class ContextoCaso {

    private final SesionEjecucion sesion;
    private final BitacoraCaso bitacora;
    private final ClienteApiQA api;

    ContextoCaso(SesionEjecucion sesion, BitacoraCaso bitacora) {
        this.sesion = sesion;
        this.bitacora = bitacora;
        this.api = new ClienteApiQA(sesion, bitacora);
    }

    public String codigo() {
        return bitacora.codigo();
    }

    public UUID idEjecucion() {
        return sesion.idEjecucion();
    }

    /**
     * Marca unica de esta ejecucion para los datos que crea la prueba
     * (por ejemplo ?qa=qa-1a2b3c4d en una URL), asi se pueden encontrar despues.
     */
    public String marca() {
        return "qa-" + sesion.idEjecucion().toString().substring(0, 8);
    }

    public EntornoQA entorno() {
        return sesion.entorno();
    }

    public ClienteApiQA api() {
        return api;
    }

    /** Lote etiquetado de qa_lote_robustez (activas y aprobadas). */
    public List<UrlEtiquetada> loteRobustez() {
        return sesion.lote().listarActivasAprobadas();
    }

    /** true si se pidio detener la ejecucion: las pruebas largas (soak) lo consultan para cortar. */
    public boolean detenida() {
        return sesion.detenida();
    }

    public Path carpetaEvidencias() {
        return sesion.entorno().carpetaEvidencias().resolve(sesion.idEjecucion().toString());
    }

    // ------------------------------------------------------------------
    // Precondiciones del entorno: si no se cumplen, el caso queda NOT_RUN
    // ------------------------------------------------------------------

    /** El caso solo necesita QA_BASE_URL. */
    public void requiereBackend() {
        exigir(sesion.entorno().faltantes(false));
    }

    /** El caso necesita QA_BASE_URL, QA_TEST_USER y QA_TEST_PASSWORD. */
    public void requiereSesion() {
        exigir(sesion.entorno().faltantes(true));
    }

    /** Caso de interfaz: necesita QA_FRONTEND_URL y, si inicia sesion, el usuario de prueba. */
    public void requiereFrontend(boolean necesitaSesion) {
        List<String> faltan = new java.util.ArrayList<>(sesion.entorno().faltantes(necesitaSesion));
        faltan.remove("QA_BASE_URL");
        if (EntornoQA.vacio(sesion.entorno().frontendUrl())) {
            faltan.addFirst("QA_FRONTEND_URL");
        }
        exigir(faltan);
    }

    /** El caso necesita la URL del servicio de IA (IA_URL). */
    public void requiereServicioIa() {
        if (EntornoQA.vacio(sesion.entorno().iaUrl())) {
            exigir(List.of("IA_URL"));
        }
    }

    private static void exigir(List<String> faltantes) {
        if (!faltantes.isEmpty()) {
            throw new CasoNoEjecutableException("Falta configurar " + String.join(", ", faltantes)
                    + " (variables de entorno del backend; ver backend/.env.example)");
        }
    }

    // ------------------------------------------------------------------
    // Parametros de la ejecucion
    // ------------------------------------------------------------------

    public String parametro(String nombre, String porDefecto) {
        String valor = sesion.plan().parametros().get(nombre);
        return valor == null || valor.isBlank() ? porDefecto : valor.strip();
    }

    public int parametroEntero(String nombre, int porDefecto) {
        String valor = parametro(nombre, null);
        if (valor == null) {
            return porDefecto;
        }
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException ex) {
            throw new CasoNoEjecutableException("El parámetro " + nombre + " debe ser un número entero: " + valor);
        }
    }

    public double parametroDecimal(String nombre, double porDefecto) {
        String valor = parametro(nombre, null);
        if (valor == null) {
            return porDefecto;
        }
        try {
            return Double.parseDouble(valor.replace(',', '.'));
        } catch (NumberFormatException ex) {
            throw new CasoNoEjecutableException("El parámetro " + nombre + " debe ser un número: " + valor);
        }
    }

    // ------------------------------------------------------------------
    // Pasos y evidencia
    // ------------------------------------------------------------------

    public <T> T paso(String descripcion, Supplier<T> accion) {
        return bitacora.paso(descripcion, accion);
    }

    public void paso(String descripcion, Runnable accion) {
        bitacora.paso(descripcion, () -> {
            accion.run();
            return null;
        });
    }

    // ------------------------------------------------------------------
    // Casos de interfaz (Playwright)
    // ------------------------------------------------------------------

    /** La pestana de Chromium de este caso (headless). La primera llamada abre el navegador. */
    public NavegadorQA navegador() {
        return sesion.navegadorDelCaso();
    }

    /**
     * Un paso de interfaz: al terminar se guarda una captura de la pantalla,
     * tambien si el paso falla (asi todo FAIL de interfaz tiene su captura).
     */
    public <T> T pasoUi(String descripcion, Supplier<T> accion) {
        return bitacora.paso(descripcion, () -> {
            try {
                T valor = accion.get();
                capturar(descripcion);
                return valor;
            } catch (RuntimeException | Error ex) {
                capturar(descripcion);
                throw ex;
            }
        });
    }

    public void pasoUi(String descripcion, Runnable accion) {
        pasoUi(descripcion, () -> {
            accion.run();
            return null;
        });
    }

    /** Una captura que falla no debe tapar el resultado del paso: se anota y se sigue. */
    private void capturar(String descripcion) {
        try {
            bitacora.anotarCaptura(navegador().capturar(descripcion));
        } catch (RuntimeException ex) {
            bitacora.agregarObservacion("(no se pudo guardar la captura: " + BitacoraCaso.resumen(ex) + ")");
        }
    }

    /** Lo observado en el paso actual: "HTTP 200, resultado = seguro, riesgo = 0". */
    public void observar(String texto) {
        bitacora.observar(texto);
    }

    /** Ruta de la captura del paso actual, relativa a la carpeta de evidencias. */
    public void anotarCaptura(String rutaRelativa) {
        bitacora.anotarCaptura(rutaRelativa);
    }

    public void metrica(MetricaRendimiento metrica) {
        bitacora.metrica(metrica);
    }
}
