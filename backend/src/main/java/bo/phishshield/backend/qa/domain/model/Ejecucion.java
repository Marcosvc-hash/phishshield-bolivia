package bo.phishshield.backend.qa.domain.model;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Una ejecucion del Centro de Pruebas QA (tabla qa_ejecuciones) con el
 * resultado de cada caso. Es la raiz del agregado: los resultados solo
 * cambian a traves de ella, para que las reglas se cumplan siempre:
 *
 *  - todos los casos son de la misma suite, estan activos y no se repiten;
 *  - solo se registran resultados mientras esta EN_CURSO;
 *  - al finalizar se calcula el resultado global; si falla, los casos que
 *    no corrieron quedan en NOT_RUN con el motivo.
 */
public class Ejecucion {

    public static final int MAXIMO_AMBIENTE = 40;
    private static final DateTimeFormatter FECHA_ASUNTO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Long id;
    private final UUID uuidPublico;
    private final Long idUsuario;
    private final Suite suite;
    private final AlcanceEjecucion alcance;
    private final CorreoDestino correo;
    private final String ambiente;
    private final Map<String, String> parametros;
    private EstadoEjecucion estado;
    private ResultadoGlobal resultadoGlobal;
    private final OffsetDateTime creadaEn;
    private OffsetDateTime inicio;
    private OffsetDateTime fin;
    private String mensajeError;
    private EstadoCorreo estadoCorreo;
    private String errorCorreo;
    private OffsetDateTime correoEnviadoEn;
    private String rutaReportePdf;
    private String rutaReporteHtml;
    private final List<ResultadoCaso> resultados;

    /** Para reconstruir la ejecucion desde la base. */
    public Ejecucion(Long id, UUID uuidPublico, Long idUsuario, Suite suite, AlcanceEjecucion alcance,
                     CorreoDestino correo, String ambiente, Map<String, String> parametros,
                     EstadoEjecucion estado, ResultadoGlobal resultadoGlobal, OffsetDateTime creadaEn,
                     OffsetDateTime inicio, OffsetDateTime fin, String mensajeError,
                     EstadoCorreo estadoCorreo, String errorCorreo, OffsetDateTime correoEnviadoEn,
                     String rutaReportePdf, String rutaReporteHtml, List<ResultadoCaso> resultados) {
        if (uuidPublico == null || suite == null || alcance == null || correo == null
                || estado == null || estadoCorreo == null || creadaEn == null) {
            throw new IllegalArgumentException("Faltan datos obligatorios de la ejecución");
        }
        this.id = id;
        this.uuidPublico = uuidPublico;
        this.idUsuario = idUsuario;
        this.suite = suite;
        this.alcance = alcance;
        this.correo = correo;
        this.ambiente = ambiente == null || ambiente.isBlank()
                ? "local" : Textos.recortar(ambiente.strip(), MAXIMO_AMBIENTE);
        this.parametros = Collections.unmodifiableMap(
                new LinkedHashMap<>(parametros == null ? Map.of() : parametros));
        this.estado = estado;
        this.resultadoGlobal = resultadoGlobal;
        this.creadaEn = creadaEn;
        this.inicio = inicio;
        this.fin = fin;
        this.mensajeError = mensajeError;
        this.estadoCorreo = estadoCorreo;
        this.errorCorreo = errorCorreo;
        this.correoEnviadoEn = correoEnviadoEn;
        this.rutaReportePdf = rutaReportePdf;
        this.rutaReporteHtml = rutaReporteHtml;
        this.resultados = new ArrayList<>(resultados == null ? List.of() : resultados);
    }

    /**
     * Crea una ejecucion PENDIENTE con un resultado NOT_RUN por cada caso,
     * en el orden del catalogo (no en el orden en que se marcaron).
     */
    public static Ejecucion nueva(Long idUsuario, Suite suite, AlcanceEjecucion alcance, CorreoDestino correo,
                                  String ambiente, Map<String, String> parametros,
                                  List<CasoPrueba> casos, OffsetDateTime ahora) {
        if (suite == null || alcance == null || correo == null || ahora == null) {
            throw new IllegalArgumentException("Faltan datos para crear la ejecución");
        }
        validarSeleccion(suite, casos);

        List<CasoPrueba> ordenados = casos.stream()
                .sorted(Comparator.comparingInt(CasoPrueba::orden).thenComparing(CasoPrueba::codigo))
                .toList();
        List<ResultadoCaso> pendientes = new ArrayList<>();
        for (int i = 0; i < ordenados.size(); i++) {
            pendientes.add(ResultadoCaso.pendiente(ordenados.get(i), i + 1));
        }

        return new Ejecucion(null, UUID.randomUUID(), idUsuario, suite, alcance, correo, ambiente, parametros,
                EstadoEjecucion.PENDIENTE, null, ahora, null, null, null,
                EstadoCorreo.PENDIENTE, null, null, null, null, pendientes);
    }

    private static void validarSeleccion(Suite suite, List<CasoPrueba> casos) {
        if (casos == null || casos.isEmpty()) {
            throw new SeleccionInvalidaException("Seleccione al menos un caso para ejecutar");
        }
        Set<String> vistos = new HashSet<>();
        for (CasoPrueba caso : casos) {
            if (caso.suite() != suite) {
                throw new SeleccionInvalidaException("El caso " + caso.codigo()
                        + " no pertenece a la suite " + suite.etiqueta());
            }
            if (!caso.activo()) {
                throw new SeleccionInvalidaException("El caso " + caso.codigo() + " está desactivado");
            }
            if (!vistos.add(caso.codigo())) {
                throw new SeleccionInvalidaException("El caso " + caso.codigo() + " está repetido");
            }
        }
    }

    // ------------------------------------------------------------------
    // Ciclo de vida
    // ------------------------------------------------------------------

    public void iniciar(OffsetDateTime ahora) {
        pasarA(EstadoEjecucion.EN_CURSO);
        this.inicio = ahora;
    }

    /** Guarda el desenlace de un caso. Solo con la ejecucion en curso. */
    public void registrarResultado(DesenlaceCaso desenlace) {
        if (estado != EstadoEjecucion.EN_CURSO) {
            throw new TransicionEjecucionInvalidaException(
                    "Solo se registran resultados con la ejecución en curso (está " + estado + ")");
        }
        resultados.stream()
                .filter(r -> r.getCodigo().equals(desenlace.codigo()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "El caso " + desenlace.codigo() + " no forma parte de esta ejecución"))
                .registrar(desenlace);
    }

    /** Todos los casos corrieron (o se omitieron): se calcula el resultado global. */
    public void finalizar(OffsetDateTime ahora) {
        pasarA(EstadoEjecucion.FINALIZADA);
        this.fin = noAntesDelInicio(ahora);
        this.resultadoGlobal = ResultadoGlobal.calcular(resultados.stream().map(ResultadoCaso::getEstado).toList());
    }

    /**
     * La ejecucion se rompio o se interrumpio. Los casos que no alcanzaron a
     * correr quedan en NOT_RUN con el motivo; no hay resultado global.
     */
    public void fallar(String motivo, OffsetDateTime ahora) {
        pasarA(EstadoEjecucion.FALLIDA);
        this.fin = noAntesDelInicio(ahora);
        this.mensajeError = Textos.mensaje(motivo == null ? "La ejecución terminó sin completar" : motivo);
        resultados.forEach(r -> r.anotarNoEjecutado("No se ejecutó: " + this.mensajeError));
    }

    private void pasarA(EstadoEjecucion destino) {
        if (!estado.puedePasarA(destino)) {
            throw new TransicionEjecucionInvalidaException(
                    "La ejecución no puede pasar de " + estado + " a " + destino);
        }
        this.estado = destino;
    }

    /** chk_qa_ejec_fechas: el fin nunca queda antes del inicio. */
    private OffsetDateTime noAntesDelInicio(OffsetDateTime ahora) {
        return inicio != null && ahora.isBefore(inicio) ? inicio : ahora;
    }

    // ------------------------------------------------------------------
    // Reportes y correo: solo cuando la ejecucion ya termino
    // ------------------------------------------------------------------

    public void registrarReportes(ArchivosReporte archivos) {
        exigirTerminada("registrar los reportes");
        this.rutaReportePdf = archivos.rutaPdf();
        this.rutaReporteHtml = archivos.rutaHtml();
    }

    public void marcarCorreoEnviado(OffsetDateTime ahora) {
        exigirTerminada("enviar el correo");
        this.estadoCorreo = EstadoCorreo.ENVIADO;
        this.errorCorreo = null;
        this.correoEnviadoEn = ahora;
    }

    /** El envio fallo: queda registrado y los reportes siguen disponibles. */
    public void marcarErrorCorreo(String error) {
        exigirTerminada("registrar el error de correo");
        this.estadoCorreo = EstadoCorreo.ERROR;
        this.errorCorreo = Textos.mensaje(error == null ? "Error desconocido al enviar el correo" : error);
    }

    public void marcarCorreoNoConfigurado() {
        exigirTerminada("registrar el estado del correo");
        this.estadoCorreo = EstadoCorreo.NO_CONFIGURADO;
        this.errorCorreo = "No hay servidor SMTP configurado (variables MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD)";
    }

    private void exigirTerminada(String accion) {
        if (!estado.esFinal()) {
            throw new TransicionEjecucionInvalidaException(
                    "No se puede " + accion + " de una ejecución que no terminó (está " + estado + ")");
        }
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    public TipoPrueba tipo() {
        return suite.tipo();
    }

    public ResumenEjecucion resumen() {
        return ResumenEjecucion.de(resultados.stream().map(ResultadoCaso::getEstado).toList());
    }

    /** null mientras no tenga inicio y fin. */
    public Long duracionMs() {
        return inicio == null || fin == null ? null : Duration.between(inicio, fin).toMillis();
    }

    public List<String> codigos() {
        return resultados.stream().map(ResultadoCaso::getCodigo).toList();
    }

    public boolean tieneReportes() {
        return rutaReportePdf != null && rutaReporteHtml != null;
    }

    /** "Reporte de automatización – Smoke – 17/11/2026 21:05" (hora de la zona indicada). */
    public String asuntoCorreo(ZoneId zona) {
        OffsetDateTime cuando = inicio != null ? inicio : creadaEn;
        return "Reporte de automatización – " + suite.etiqueta() + " – "
                + cuando.atZoneSameInstant(zona).format(FECHA_ASUNTO);
    }

    public Long getId() { return id; }
    public UUID getUuidPublico() { return uuidPublico; }
    public Long getIdUsuario() { return idUsuario; }
    public Suite getSuite() { return suite; }
    public AlcanceEjecucion getAlcance() { return alcance; }
    public CorreoDestino getCorreo() { return correo; }
    public String getAmbiente() { return ambiente; }
    public Map<String, String> getParametros() { return parametros; }
    public EstadoEjecucion getEstado() { return estado; }
    public ResultadoGlobal getResultadoGlobal() { return resultadoGlobal; }
    public OffsetDateTime getCreadaEn() { return creadaEn; }
    public OffsetDateTime getInicio() { return inicio; }
    public OffsetDateTime getFin() { return fin; }
    public String getMensajeError() { return mensajeError; }
    public EstadoCorreo getEstadoCorreo() { return estadoCorreo; }
    public String getErrorCorreo() { return errorCorreo; }
    public OffsetDateTime getCorreoEnviadoEn() { return correoEnviadoEn; }
    public String getRutaReportePdf() { return rutaReportePdf; }
    public String getRutaReporteHtml() { return rutaReporteHtml; }
    public List<ResultadoCaso> getResultados() { return List.copyOf(resultados); }
}
