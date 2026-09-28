package bo.phishshield.backend.analisis.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "entidades_suplantadas")
public class EntidadSuplantadaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_entidad")
    private Long idEntidad;

    @Column(name = "nombre", nullable = false, unique = true, length = 120)
    private String nombre;

    @Column(name = "tipo", nullable = false, length = 30)
    private String tipo;

    @Column(name = "dominio_oficial", length = 150)
    private String dominioOficial;

    public Long getIdEntidad() { return idEntidad; }
    public void setIdEntidad(Long idEntidad) { this.idEntidad = idEntidad; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getDominioOficial() { return dominioOficial; }
    public void setDominioOficial(String dominioOficial) { this.dominioOficial = dominioOficial; }
}