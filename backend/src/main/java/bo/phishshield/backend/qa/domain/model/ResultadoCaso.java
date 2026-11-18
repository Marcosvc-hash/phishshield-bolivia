package bo.phishshield.backend.qa.domain.model;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * El resultado de un caso dentro de una ejecucion (tabla qa_resultados).
 * Nace en NOT_RUN al crear la ejecucion y recibe su desenlace una sola vez.
 */
public class ResultadoCaso {

    private final Long id;
    private final CasoPrueba caso;
    private final int orden;
    private EstadoCaso estado;
    private TipoFallo tipoFallo;
    private String mensajeError;
    private String pasoFallido;
    private String observacion;
    private OffsetDateTime inicio;
    private OffsetDateTime fin;
    private final List<PasoEjecutado> pasos;
    private final List<MetricaRendimiento> metricas;

    /** Para reconstruir el resultado desde la base. */
    public ResultadoCaso(Long id, CasoPrueba caso, int orden, EstadoCaso estado, TipoFallo tipoFallo,
                         String mensajeError, String pasoFallido, String observacion,
                         OffsetDateTime inicio, OffsetDateTime fin,
                         List<PasoEjecutado> pasos, List<MetricaRendimiento> metricas) {
        if (caso == null || estado == null) {
            throw new IllegalArgumentException("El resultado necesita su caso y su estado");
        }
        if ((estado == EstadoCaso.FAIL) != (tipoFallo != null)) {
            throw new IllegalArgumentException("El tipo de fallo va solo, y siempre, en los casos FAIL");
        }
        this.id = id;
        this.caso = caso;
        this.orden = orden;
        this.estado = estado;
        this.tipoFallo = tipoFallo;
        this.mensajeError = mensajeError;
        this.pasoFallido = pasoFallido;
        this.observacion = observacion;
        this.inicio = inicio;
        this.fin = fin;
        this.pasos = new ArrayList<>(pasos == null ? List.of() : pasos);
        this.metricas = new ArrayList<>(metricas == null ? List.of() : metricas);
    }

    /** Un caso recien seleccionado: todavia no corre. */
    public static ResultadoCaso pendiente(CasoPrueba caso, int orden) {
        return new ResultadoCaso(null, caso, orden, EstadoCaso.NOT_RUN, null,
                null, null, null, null, null, List.of(), List.of());
    }

    /** Guarda lo que informo el ejecutor. Un caso no puede recibir dos desenlaces. */
    void registrar(DesenlaceCaso desenlace) {
        if (!caso.codigo().equals(desenlace.codigo())) {
            throw new IllegalArgumentException("El desenlace de " + desenlace.codigo()
                    + " no corresponde al caso " + caso.codigo());
        }
        if (estado != EstadoCaso.NOT_RUN) {
            throw new IllegalStateException("El caso " + caso.codigo() + " ya tiene resultado (" + estado + ")");
        }
        this.estado = desenlace.estado();
        this.tipoFallo = desenlace.tipoFallo();
        this.mensajeError = desenlace.mensajeError();
        this.pasoFallido = desenlace.pasoFallido();
        this.observacion = desenlace.observacion();
        this.inicio = desenlace.inicio();
        this.fin = desenlace.fin();
        this.pasos.clear();
        this.pasos.addAll(desenlace.pasos());
        this.metricas.clear();
        this.metricas.addAll(desenlace.metricas());
    }

    /** Explica por que un caso quedo sin ejecutar (solo si sigue en NOT_RUN). */
    void anotarNoEjecutado(String motivo) {
        if (estado == EstadoCaso.NOT_RUN) {
            this.observacion = Textos.mensaje(motivo);
        }
    }

    /**
     * Lo que va en la columna "Observación" del reporte resumido.
     * En PASS es siempre "Sin observaciones".
     */
    public String observacionResumida() {
        return switch (estado) {
            case PASS -> "Sin observaciones";
            case FAIL -> "Fallo " + tipoFallo.etiqueta().toLowerCase()
                    + (pasoFallido != null ? " en el paso \"" + pasoFallido + "\"" : "")
                    + (mensajeError != null ? ": " + primeraLinea(mensajeError) : "");
            case SKIPPED -> observacion != null ? observacion : "Omitido sin motivo registrado";
            case NOT_RUN -> observacion != null ? observacion : "No se ejecutó";
        };
    }

    /** null mientras el caso no tenga inicio y fin. */
    public Long duracionMs() {
        return inicio == null || fin == null ? null : Duration.between(inicio, fin).toMillis();
    }

    private static String primeraLinea(String texto) {
        int salto = texto.indexOf('\n');
        return salto < 0 ? texto : texto.substring(0, salto).strip();
    }

    public Long getId() { return id; }
    public CasoPrueba getCaso() { return caso; }
    public String getCodigo() { return caso.codigo(); }
    public int getOrden() { return orden; }
    public EstadoCaso getEstado() { return estado; }
    public TipoFallo getTipoFallo() { return tipoFallo; }
    public String getMensajeError() { return mensajeError; }
    public String getPasoFallido() { return pasoFallido; }
    public String getObservacion() { return observacion; }
    public OffsetDateTime getInicio() { return inicio; }
    public OffsetDateTime getFin() { return fin; }
    public List<PasoEjecutado> getPasos() { return List.copyOf(pasos); }
    public List<MetricaRendimiento> getMetricas() { return List.copyOf(metricas); }
}
