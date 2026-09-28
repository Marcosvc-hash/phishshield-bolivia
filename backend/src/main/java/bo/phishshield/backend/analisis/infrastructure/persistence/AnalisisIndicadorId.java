package bo.phishshield.backend.analisis.infrastructure.persistence;

import java.io.Serializable;
import java.util.Objects;

/**
 * Clave primaria compuesta de analisis_indicadores.
 * JPA exige que sea Serializable y que implemente equals y hashCode.
 */
public class AnalisisIndicadorId implements Serializable {

    private Long idAnalisis;
    private Long idRegla;

    public AnalisisIndicadorId() {
    }

    public AnalisisIndicadorId(Long idAnalisis, Long idRegla) {
        this.idAnalisis = idAnalisis;
        this.idRegla = idRegla;
    }

    public Long getIdAnalisis() { return idAnalisis; }
    public void setIdAnalisis(Long idAnalisis) { this.idAnalisis = idAnalisis; }

    public Long getIdRegla() { return idRegla; }
    public void setIdRegla(Long idRegla) { this.idRegla = idRegla; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AnalisisIndicadorId otro)) return false;
        return Objects.equals(idAnalisis, otro.idAnalisis)
                && Objects.equals(idRegla, otro.idRegla);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idAnalisis, idRegla);
    }
}