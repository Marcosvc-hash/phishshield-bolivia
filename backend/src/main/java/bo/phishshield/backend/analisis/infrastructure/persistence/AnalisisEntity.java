package bo.phishshield.backend.analisis.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "analisis")
public class AnalisisEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_analisis")
    private Long idAnalisis;

    @Column(name = "uuid_publico", nullable = false, unique = true)
    private UUID uuidPublico;

    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "id_empresa")
    private Long idEmpresa;

    @Column(name = "id_modelo")
    private Long idModelo;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;

    @Column(name = "origen", nullable = false, length = 20)
    private String origen;

    @Column(name = "contenido", nullable = false, columnDefinition = "TEXT")
    private String contenido;

    @Column(name = "contenido_hash", nullable = false, length = 64, columnDefinition = "bpchar")
    private String contenidoHash;

    @Column(name = "dominio", length = 255)
    private String dominio;

    @Column(name = "resultado", nullable = false, length = 20)
    private String resultado;

    @Column(name = "nivel_riesgo", nullable = false, precision = 5, scale = 2)
    private BigDecimal nivelRiesgo;

    @Column(name = "tiempo_ms")
    private Integer tiempoMs;

    @Column(name = "fecha_analisis", nullable = false)
    private OffsetDateTime fechaAnalisis;

    public Long getIdAnalisis() { return idAnalisis; }
    public void setIdAnalisis(Long idAnalisis) { this.idAnalisis = idAnalisis; }

    public UUID getUuidPublico() { return uuidPublico; }
    public void setUuidPublico(UUID uuidPublico) { this.uuidPublico = uuidPublico; }

    public Long getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }

    public Long getIdEmpresa() { return idEmpresa; }
    public void setIdEmpresa(Long idEmpresa) { this.idEmpresa = idEmpresa; }

    public Long getIdModelo() { return idModelo; }
    public void setIdModelo(Long idModelo) { this.idModelo = idModelo; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }

    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }

    public String getContenidoHash() { return contenidoHash; }
    public void setContenidoHash(String contenidoHash) { this.contenidoHash = contenidoHash; }

    public String getDominio() { return dominio; }
    public void setDominio(String dominio) { this.dominio = dominio; }

    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }

    public BigDecimal getNivelRiesgo() { return nivelRiesgo; }
    public void setNivelRiesgo(BigDecimal nivelRiesgo) { this.nivelRiesgo = nivelRiesgo; }

    public Integer getTiempoMs() { return tiempoMs; }
    public void setTiempoMs(Integer tiempoMs) { this.tiempoMs = tiempoMs; }

    public OffsetDateTime getFechaAnalisis() { return fechaAnalisis; }
    public void setFechaAnalisis(OffsetDateTime fechaAnalisis) { this.fechaAnalisis = fechaAnalisis; }
}