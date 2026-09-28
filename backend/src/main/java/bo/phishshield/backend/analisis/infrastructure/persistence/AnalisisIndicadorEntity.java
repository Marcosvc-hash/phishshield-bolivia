package bo.phishshield.backend.analisis.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "analisis_indicadores")
@IdClass(AnalisisIndicadorId.class)
public class AnalisisIndicadorEntity {

    @Id
    @Column(name = "id_analisis")
    private Long idAnalisis;

    @Id
    @Column(name = "id_regla")
    private Long idRegla;

    @Column(name = "valor", length = 255)
    private String valor;

    @Column(name = "aporte", nullable = false, precision = 5, scale = 2)
    private BigDecimal aporte;

    public Long getIdAnalisis() { return idAnalisis; }
    public void setIdAnalisis(Long idAnalisis) { this.idAnalisis = idAnalisis; }

    public Long getIdRegla() { return idRegla; }
    public void setIdRegla(Long idRegla) { this.idRegla = idRegla; }

    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }

    public BigDecimal getAporte() { return aporte; }
    public void setAporte(BigDecimal aporte) { this.aporte = aporte; }
}