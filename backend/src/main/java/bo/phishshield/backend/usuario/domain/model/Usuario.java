package bo.phishshield.backend.usuario.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public class Usuario {

    private final Long id;
    private final UUID uuidPublico;
    private final Long idEmpresa;
    private final String nombres;
    private final String apellidos;
    private final String email;
    private final String rol;
    private final String estado;
    private final boolean emailVerificado;
    private final OffsetDateTime ultimoLogin;
    private final OffsetDateTime creadoEn;

    public Usuario(Long id, UUID uuidPublico, Long idEmpresa,
                   String nombres, String apellidos, String email,
                   String rol, String estado, boolean emailVerificado,
                   OffsetDateTime ultimoLogin, OffsetDateTime creadoEn) {

        if (nombres == null || nombres.isBlank()) {
            throw new IllegalArgumentException("Los nombres del usuario son obligatorios");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El email es obligatorio");
        }
        if (!email.equals(email.toLowerCase())) {
            throw new IllegalArgumentException("El email debe estar en minusculas: " + email);
        }
        if (!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new IllegalArgumentException("Formato de email invalido: " + email);
        }
        if (rol == null || !rol.matches("admin|analista|empresa|usuario")) {
            throw new IllegalArgumentException("Rol invalido: " + rol);
        }
        if (estado == null || !estado.matches("activo|inactivo|bloqueado|pendiente")) {
            throw new IllegalArgumentException("Estado invalido: " + estado);
        }

        this.id = id;
        this.uuidPublico = uuidPublico;
        this.idEmpresa = idEmpresa;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.email = email;
        this.rol = rol;
        this.estado = estado;
        this.emailVerificado = emailVerificado;
        this.ultimoLogin = ultimoLogin;
        this.creadoEn = creadoEn;
    }

    public boolean puedeIniciarSesion() {
        return "activo".equals(estado) && emailVerificado;
    }

    public String nombreCompleto() {
        return apellidos == null || apellidos.isBlank()
                ? nombres
                : nombres + " " + apellidos;
    }

    public Long getId() { return id; }
    public UUID getUuidPublico() { return uuidPublico; }
    public Long getIdEmpresa() { return idEmpresa; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public String getEmail() { return email; }
    public String getRol() { return rol; }
    public String getEstado() { return estado; }
    public boolean isEmailVerificado() { return emailVerificado; }
    public OffsetDateTime getUltimoLogin() { return ultimoLogin; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
}