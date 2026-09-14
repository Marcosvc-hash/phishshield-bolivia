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
            throw new IllegalArgumentException("El origen del analisis es obligatorio");
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

    /** Reconstruye un analisis leido de la base de datos. Lo usa el adaptador de persistencia. */
    public static Analisis desdePersistencia(Long id, UUID uuidPublico, Long idUsuario,
                                             Long idEmpresa, Long idModelo,
                                             TipoAnalisis tipo, OrigenAnalisis origen,
                                             String contenido, String contenidoHash,
                                             String dominio, BigDecimal nivelRiesgo,
                                             Integer tiempoMs, OffsetDateTime fechaAnalisis,
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
        return a;
    }

    public boolean requiereAlerta() {
        return resultado != Resultado.SEGURO;
    }

    // ---- Contexto que asigna la infraestructura, no el dominio ----

    public void asignarContexto(Long idUsuario, Long idEmpresa, Long idModelo) {
        this.idUsuario = idUsuario;
        this.idEmpresa = idEmpresa;
        this.idModelo = idModelo;
    }

    public void registrarTiempo(int milisegundos) {
        if (milisegundos < 0) {
            throw new IllegalArgumentException("El tiempo de analisis no puede ser negativo");
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
}