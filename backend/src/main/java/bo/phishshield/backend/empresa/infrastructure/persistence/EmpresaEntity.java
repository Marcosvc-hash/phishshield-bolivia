package bo.phishshield.backend.empresa.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "empresas")
@Getter
@Setter
@NoArgsConstructor
public class EmpresaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_empresa")
    private Long idEmpresa;

    @Column(name = "uuid_publico", nullable = false, updatable = false)
    private UUID uuidPublico;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "nit", length = 20)
    private String nit;

    @Column(name = "sector", length = 60)
    private String sector;

    @Column(name = "ciudad", length = 60)
    private String ciudad;

    @Column(name = "plan", nullable = false, length = 20)
    private String plan;

    @Column(name = "activa", nullable = false)
    private Boolean activa;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;
}