package bo.phishshield.backend.qa.infrastructure.ejecutor;

import bo.phishshield.backend.qa.domain.model.EstadoPaso;
import bo.phishshield.backend.qa.domain.model.MetricaRendimiento;
import bo.phishshield.backend.qa.domain.model.PasoEjecutado;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Lo que va registrando un caso mientras corre: sus pasos (con lo observado
 * y la evidencia) y sus metricas. Al terminar, el ejecutor arma el
 * DesenlaceCaso con esto.
 */
public final class BitacoraCaso {

    /** Detalle de API por paso: suficiente para leer la peticion y la respuesta, no un volcado entero. */
    private static final int MAXIMO_DETALLE = 6000;

    private final String codigo;
    private final Clock reloj;
    private final List<PasoEjecutado> pasos = new ArrayList<>();
    private final List<MetricaRendimiento> metricas = new ArrayList<>();

    // El paso que esta corriendo ahora
    private String observado;
    private final StringBuilder detalleApi = new StringBuilder();
    private String captura;
    private String pasoFallido;

    public BitacoraCaso(String codigo, Clock reloj) {
        this.codigo = codigo;
        this.reloj = reloj;
    }

    public <T> T paso(String descripcion, Supplier<T> accion) {
        observado = null;
        detalleApi.setLength(0);
        captura = null;
        try {
            T valor = accion.get();
            registrar(descripcion, EstadoPaso.OK, observado);
            return valor;
        } catch (RuntimeException | Error ex) {
            String visto = observado != null
                    ? observado + " | " + resumen(ex)
                    : resumen(ex);
            registrar(descripcion, EstadoPaso.FALLO, visto);
            pasoFallido = descripcion;
            throw ex;
        }
    }

    /** Lo que se vio en el paso actual (va a la columna "Resultado observado"). */
    public void observar(String texto) {
        observado = texto;
    }

    /** La peticion y la respuesta de API del paso actual (ya enmascaradas). */
    public void anotarDetalleApi(String detalle) {
        if (detalleApi.length() > 0) {
            detalleApi.append("\n\n");
        }
        detalleApi.append(detalle);
        if (detalleApi.length() > MAXIMO_DETALLE) {
            detalleApi.setLength(MAXIMO_DETALLE - 1);
            detalleApi.append('…');
        }
    }

    /** Ruta de la captura del paso actual, relativa a la carpeta de evidencias. */
    public void anotarCaptura(String rutaRelativa) {
        captura = rutaRelativa;
    }

    public void metrica(MetricaRendimiento metrica) {
        metricas.add(metrica);
    }

    private void registrar(String descripcion, EstadoPaso estado, String visto) {
        pasos.add(new PasoEjecutado(pasos.size() + 1, descripcion, visto, estado, captura,
                detalleApi.isEmpty() ? null : detalleApi.toString(), OffsetDateTime.now(reloj)));
    }

    static String resumen(Throwable ex) {
        String mensaje = ex.getMessage();
        if (mensaje == null || mensaje.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return ex instanceof AssertionError || ex instanceof FalloInfraestructuraException
                ? mensaje.strip()
                : ex.getClass().getSimpleName() + ": " + mensaje.strip();
    }

    public String codigo() {
        return codigo;
    }

    public List<PasoEjecutado> pasos() {
        return List.copyOf(pasos);
    }

    public List<MetricaRendimiento> metricas() {
        return List.copyOf(metricas);
    }

    /** Descripcion del primer paso que fallo; null si el fallo ocurrio fuera de un paso. */
    public String pasoFallido() {
        return pasoFallido;
    }
}
