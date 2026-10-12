package bo.phishshield.backend.analisis.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Mapeo parcial de alertas: solo lo que escribe el motor.
 * fecha_notificacion la pone la base (DEFAULT NOW()); la asignacion,
 * las notas y el cierre quedan para el modulo de gestion de alertas.
 */
@Entity
@Table(name = "alertas")
public class AlertaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alerta")
    private Long idAlerta;

    @Column(name = "id_analisis", nullable = false, unique = true)
    private Long idAnalisis;

    @Column(name = "severidad", nullable = false, length = 10)
    private String severidad;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "canal", nullable = false, length = 20)
    private String canal;

    public Long getIdAlerta() { return idAlerta; }
    public void setIdAlerta(Long idAlerta) { this.idAlerta = idAlerta; }

    public Long getIdAnalisis() { return idAnalisis; }
    public void setIdAnalisis(Long idAnalisis) { this.idAnalisis = idAnalisis; }

    public String getSeveridad() { return severidad; }
    public void setSeveridad(String severidad) { this.severidad = severidad; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getCanal() { return canal; }
    public void setCanal(String canal) { this.canal = canal; }
}
