package bo.phishshield.backend.analisis.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Mapeo parcial de dominios_lista: solo las columnas que usa el motor.
 * Las demas (id_entidad, fuente, creado_en) tienen valor por defecto
 * en la base y este backend no escribe en la tabla.
 */
@Entity
@Table(name = "dominios_lista")
public class DominioListaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dominio")
    private Long idDominio;

    @Column(name = "dominio", nullable = false, length = 255)
    private String dominio;

    @Column(name = "lista", nullable = false, length = 10)
    private String lista;

    @Column(name = "motivo", columnDefinition = "TEXT")
    private String motivo;

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    public Long getIdDominio() { return idDominio; }
    public void setIdDominio(Long idDominio) { this.idDominio = idDominio; }

    public String getDominio() { return dominio; }
    public void setDominio(String dominio) { this.dominio = dominio; }

    public String getLista() { return lista; }
    public void setLista(String lista) { this.lista = lista; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
