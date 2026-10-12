package bo.phishshield.backend.analisis.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Solo las columnas de modelos_ia que necesita el backend; el registro lo hacen las migraciones. */
@Entity
@Table(name = "modelos_ia")
public class ModeloIaEntity {

    @Id
    @Column(name = "id_modelo")
    private Long idModelo;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @Column(name = "version", nullable = false, length = 20)
    private String version;

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    public Long getIdModelo() { return idModelo; }
    public String getNombre() { return nombre; }
    public String getVersion() { return version; }
    public Boolean getActivo() { return activo; }
}
