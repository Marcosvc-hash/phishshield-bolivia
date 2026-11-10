package bo.phishshield.backend.usuario.infrastructure.persistence;

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

/**
 * Mapeo parcial de sesiones. ip_origen (INET) no se mapea: la IP de cada
 * acceso ya queda en auditoria. creado_en lo pone la base (DEFAULT NOW()).
 */
@Entity
@Table(name = "sesiones")
@Getter
@Setter
@NoArgsConstructor
public class SesionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sesion")
    private Long idSesion;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(name = "refresh_token_hash", nullable = false, unique = true, length = 255)
    private String refreshTokenHash;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "expira_en", nullable = false)
    private OffsetDateTime expiraEn;

    @Column(name = "revocada", nullable = false)
    private Boolean revocada;

    @Column(name = "creado_en", insertable = false, updatable = false)
    private OffsetDateTime creadoEn;
}
