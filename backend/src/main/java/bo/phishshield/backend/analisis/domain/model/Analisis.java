package bo.phishshield.backend.analisis.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Agregado principal de la vertical. Un analisis nunca se construye
 * con un veredicto arbitrario: el veredicto se deriva del puntaje,
 * y el puntaje se deriva de los indicadores.
 */
public final class Analisis {

    private static final BigDecimal MAXIMO = new BigDecimal("100");

    private Long id;
    private UUID uuidPublico;
    private Long idUsuario;
    private Long idEmpresa;
    private Long idModelo;
    private Integer tiempoMs;
    private OffsetDateTime fechaAnalisis;
    /**
     * Si los indicadores se copiaron de un analisis anterior (cache), cuando
     * se calcularon. Es null cuando este analisis los calculo el mismo.
     */
    private OffsetDateTime calculadoEn;

    private final TipoAnalisis tipo;
    private final OrigenAnalisis origen;
    private final String contenido;
    private final String contenidoHash;
    private final String dominio;
    private final BigDecimal nivelRiesgo;
    private final Resultado resultado;
    private final List<Indicador> indicadores;

    private Analisis(TipoAnalisis tipo, OrigenAnalisis origen, String contenido,
                     String contenidoHash, String dominio,
                     BigDecimal nivelRiesgo, List<Indicador> indicadores) {
        this.tipo = tipo;
        this.origen = origen;
        this.contenido = contenido;
        this.contenidoHash = contenidoHash;
        this.dominio = dominio;
        this.nivelRiesgo = nivelRiesgo;
        this.resultado = Resultado.desdePuntaje(nivelRiesgo);
        this.indicadores = List.copyOf(indicadores);
    }

    /** Fabrica para analisis de URL: el puntaje y el veredicto salen de los indicadores. */
    public static Analisis deUrl(UrlAnalizada url, OrigenAnalisis origen,
                                 List<Indicador> indicadores) {
        if (url == null) {
            throw new IllegalArgumentException("La URL analizada es obligatoria");
        }
        if (origen == null) {
            throw new IllegalArgumentException("El origen del análisis es obligatorio");
        }
        if (indicadores == null) {
            throw new IllegalArgumentException("La lista de indicadores no puede ser nula");
        }

        BigDecimal puntaje = indicadores.stream()
                .map(Indicador::aporte)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .max(BigDecimal.ZERO)
                .min(MAXIMO)
                .setScale(2, RoundingMode.HALF_UP);

        return new Analisis(
                TipoAnalisis.URL,
                origen,
                url.getOriginal(),
                Sha256.de(url.getOriginal()),
                url.getHost(),
                puntaje,
                indicadores
        );
    }

    /**
     * Fabrica para la cache: registra el pedido de otro usuario o empresa sin
     * recalcular. Copia la URL, los indicadores y el puntaje del analisis
     * previo, y recuerda cuando se hizo ese calculo. El usuario, la empresa y
     * el modelo de IA los asigna despues el caso de uso (asignarContexto).
     *
     * La vigencia de la cache se mide desde el calculo y no desde la fecha de
     * la copia: si cada copia la renovara, una URL consultada todos los dias
     * por personas distintas no se recalcularia nunca. Por eso la copia de
     * una copia conserva la fecha del calculo original.
     */
    public static Analisis reutilizando(Analisis previo, OrigenAnalisis origen) {
        if (previo == null) {
            throw new IllegalArgumentException("El análisis previo es obligatorio");
        }
        if (origen == null) {
            throw new IllegalArgumentException("El origen del análisis es obligatorio");
        }
        if (previo.momentoDelCalculo() == null) {
            throw new IllegalArgumentException("Solo se puede reutilizar un análisis ya guardado");
        }
        Analisis copia = new Analisis(previo.tipo, origen, previo.contenido, previo.contenidoHash,
                previo.dominio, previo.nivelRiesgo, previo.indicadores);
        copia.calculadoEn = previo.momentoDelCalculo();
        return copia;
    }

    /** Reconstruye un analisis leido de la base de datos. Lo usa el adaptador de persistencia. */
    public static Analisis desdePersistencia(Long id, UUID uuidPublico, Long idUsuario,
                                             Long idEmpresa, Long idModelo,
                                             TipoAnalisis tipo, OrigenAnalisis origen,
                                             String contenido, String contenidoHash,
                                             String dominio, BigDecimal nivelRiesgo,
                                             Integer tiempoMs, OffsetDateTime fechaAnalisis,
                                             OffsetDateTime calculadoEn,
                                             List<Indicador> indicadores) {
        Analisis a = new Analisis(tipo, origen, contenido, contenidoHash,
                dominio, nivelRiesgo, indicadores);
        a.id = id;
        a.uuidPublico = uuidPublico;
        a.idUsuario = idUsuario;
        a.idEmpresa = idEmpresa;
        a.idModelo = idModelo;
        a.tiempoMs = tiempoMs;
        a.fechaAnalisis = fechaAnalisis;
        a.calculadoEn = calculadoEn;
        return a;
    }

    public boolean requiereAlerta() {
        return resultado != Resultado.SEGURO;
    }

    /**
     * Cuando se calcularon de verdad los indicadores: la fecha del calculo
     * original si es una copia de la cache, o la propia si no. Es null en un
     * analisis que todavia no se guardo.
     */
    public OffsetDateTime momentoDelCalculo() {
        return calculadoEn != null ? calculadoEn : fechaAnalisis;
    }

    // ---- Contexto que asigna la infraestructura, no el dominio ----

    public void asignarContexto(Long idUsuario, Long idEmpresa, Long idModelo) {
        this.idUsuario = idUsuario;
        this.idEmpresa = idEmpresa;
        this.idModelo = idModelo;
    }

    public void registrarTiempo(int milisegundos) {
        if (milisegundos < 0) {
            throw new IllegalArgumentException("El tiempo de análisis no puede ser negativo");
        }
        this.tiempoMs = milisegundos;
    }

    public void asignarIdentidad(Long id, UUID uuidPublico, OffsetDateTime fechaAnalisis) {
        this.id = id;
        this.uuidPublico = uuidPublico;
        this.fechaAnalisis = fechaAnalisis;
    }

    // ---- Getters ----

    public Long getId() { return id; }
    public UUID getUuidPublico() { return uuidPublico; }
    public Long getIdUsuario() { return idUsuario; }
    public Long getIdEmpresa() { return idEmpresa; }
    public Long getIdModelo() { return idModelo; }
    public TipoAnalisis getTipo() { return tipo; }
    public OrigenAnalisis getOrigen() { return origen; }
    public String getContenido() { return contenido; }
    public String getContenidoHash() { return contenidoHash; }
    public String getDominio() { return dominio; }
    public BigDecimal getNivelRiesgo() { return nivelRiesgo; }
    public Resultado getResultado() { return resultado; }
    public List<Indicador> getIndicadores() { return indicadores; }
    public Integer getTiempoMs() { return tiempoMs; }
    public OffsetDateTime getFechaAnalisis() { return fechaAnalisis; }
    public OffsetDateTime getCalculadoEn() { return calculadoEn; }
}