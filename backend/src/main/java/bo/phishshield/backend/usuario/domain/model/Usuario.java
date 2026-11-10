package bo.phishshield.backend.usuario.domain.model;

import bo.phishshield.backend.shared.domain.model.Rol;

import java.time.OffsetDateTime;
import java.util.UUID;

public class Usuario {

    /** chk_usuarios_intentos: la columna no admite mas de 100. */
    private static final int MAXIMO_INTENTOS_REGISTRADOS = 100;

    private final Long id;
    private final UUID uuidPublico;
    private final Long idEmpresa;
    private final String nombres;
    private final String apellidos;
    private final String email;
    private final String passwordHash;
    private final Rol rol;
    private EstadoUsuario estado;
    private final boolean emailVerificado;
    private int intentosFallidos;
    private OffsetDateTime ultimoLogin;
    private final OffsetDateTime creadoEn;

    public Usuario(Long id, UUID uuidPublico, Long idEmpresa,
                   String nombres, String apellidos, String email, String passwordHash,
                   Rol rol, EstadoUsuario estado, boolean emailVerificado,
                   int intentosFallidos, OffsetDateTime ultimoLogin, OffsetDateTime creadoEn) {

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
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("El usuario necesita una contrasena");
        }
        if (rol == null) {
            throw new IllegalArgumentException("El rol es obligatorio");
        }
        if (estado == null) {
            throw new IllegalArgumentException("El estado es obligatorio");
        }

        this.id = id;
        this.uuidPublico = uuidPublico;
        this.idEmpresa = idEmpresa;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.email = email;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.estado = estado;
        this.emailVerificado = emailVerificado;
        this.intentosFallidos = intentosFallidos;
        this.ultimoLogin = ultimoLogin;
        this.creadoEn = creadoEn;
    }

    /**
     * Usuario que se registra solo: siempre USUARIO, sin empresa y activo.
     * El email queda sin verificar porque todavia no se envian correos.
     */
    public static Usuario registrado(String nombres, String apellidos,
                                     String email, String passwordHash) {
        return new Usuario(null, null, null, nombres, apellidos, email, passwordHash,
                Rol.USUARIO, EstadoUsuario.ACTIVO, false, 0, null, null);
    }

    /**
     * Solo se exige que la cuenta este activa. email_verificado queda como
     * dato informativo hasta que exista el envio de correos de verificacion.
     */
    public boolean puedeIniciarSesion() {
        return estado == EstadoUsuario.ACTIVO;
    }

    public void registrarLoginExitoso(OffsetDateTime momento) {
        this.intentosFallidos = 0;
        this.ultimoLogin = momento;
    }

    public void registrarLoginFallido() {
        if (intentosFallidos < MAXIMO_INTENTOS_REGISTRADOS) {
            intentosFallidos++;
        }
    }

    public void activar() {
        this.estado = EstadoUsuario.ACTIVO;
        this.intentosFallidos = 0;
    }

    public void desactivar() {
        this.estado = EstadoUsuario.INACTIVO;
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
    public String getPasswordHash() { return passwordHash; }
    public Rol getRol() { return rol; }
    public EstadoUsuario getEstado() { return estado; }
    public boolean isEmailVerificado() { return emailVerificado; }
    public int getIntentosFallidos() { return intentosFallidos; }
    public OffsetDateTime getUltimoLogin() { return ultimoLogin; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
}
