package bo.phishshield.backend.alerta.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Una alerta vista desde la bandeja de gestion: su estado, quien la tiene
 * asignada, las notas y cuando se cerro. Las reglas del ciclo de vida
 * viven aca y en EstadoAlerta, sin nada de Spring ni de la base.
 *
 * (analisis.domain.model.Alerta es otra cosa: el aviso que el motor crea
 * cuando un analisis no es seguro.)
 */
public class AlertaGestionada {

    /** Las notas son un TEXT, pero un limite evita pegar documentos enteros. */
    public static final int MAXIMO_NOTAS = 2000;

    private final Long id;
    private final UUID uuidPublico;
    private final Long idEmpresa;
    private final SeveridadAlerta severidad;
    private final OffsetDateTime fechaNotificacion;
    private EstadoAlerta estado;
    private Long idAsignado;
    private String notas;
    private OffsetDateTime fechaCierre;

    public AlertaGestionada(Long id, UUID uuidPublico, Long idEmpresa, SeveridadAlerta severidad,
                            EstadoAlerta estado, Long idAsignado, String notas,
                            OffsetDateTime fechaNotificacion, OffsetDateTime fechaCierre) {
        if (estado == null || severidad == null) {
            throw new IllegalArgumentException("La alerta necesita estado y severidad");
        }
        this.id = id;
        this.uuidPublico = uuidPublico;
        this.idEmpresa = idEmpresa;
        this.severidad = severidad;
        this.estado = estado;
        this.idAsignado = idAsignado;
        this.notas = notas;
        this.fechaNotificacion = fechaNotificacion;
        this.fechaCierre = fechaCierre;
    }

    /**
     * Mueve la alerta al estado pedido si el ciclo de vida lo permite.
     * - Al pasar a en_revision queda asignada a quien la tomo.
     * - Al cerrarla (revisada o descartada) se guarda la fecha de cierre.
     */
    public void cambiarEstado(EstadoAlerta destino, Long idQuien, OffsetDateTime ahora) {
        if (destino == null) {
            throw new IllegalArgumentException("Indique el estado nuevo");
        }
        if (estado.esFinal()) {
            throw new TransicionAlertaInvalidaException(
                    "La alerta ya esta " + estado.aValorBd() + " y no se puede modificar");
        }
        if (!estado.puedePasarA(destino)) {
            throw new TransicionAlertaInvalidaException(
                    "Una alerta " + estado.aValorBd() + " no puede pasar a " + destino.aValorBd());
        }

        estado = destino;
        if (destino == EstadoAlerta.EN_REVISION) {
            idAsignado = idQuien;
        }
        if (destino.esFinal()) {
            fechaCierre = momentoDeCierre(ahora);
        }
    }

    /** Reemplaza las notas. Texto vacio = borrarlas. Una alerta cerrada no se toca. */
    public void anotar(String texto) {
        if (estado.esFinal()) {
            throw new TransicionAlertaInvalidaException(
                    "La alerta ya esta " + estado.aValorBd() + " y no se puede modificar");
        }
        String limpio = texto == null || texto.isBlank() ? null : texto.strip();
        if (limpio != null && limpio.length() > MAXIMO_NOTAS) {
            throw new IllegalArgumentException("Las notas no pueden superar los " + MAXIMO_NOTAS + " caracteres");
        }
        notas = limpio;
    }

    /**
     * chk_alertas_cierre exige fecha_cierre >= fecha_notificacion. Si el
     * reloj del servidor estuviera un poco atrasado respecto a la base, se
     * usa la fecha de notificacion para no violar la restriccion.
     */
    private OffsetDateTime momentoDeCierre(OffsetDateTime ahora) {
        if (fechaNotificacion != null && ahora.isBefore(fechaNotificacion)) {
            return fechaNotificacion;
        }
        return ahora;
    }

    public Long getId() { return id; }
    public UUID getUuidPublico() { return uuidPublico; }
    public Long getIdEmpresa() { return idEmpresa; }
    public SeveridadAlerta getSeveridad() { return severidad; }
    public EstadoAlerta getEstado() { return estado; }
    public Long getIdAsignado() { return idAsignado; }
    public String getNotas() { return notas; }
    public OffsetDateTime getFechaNotificacion() { return fechaNotificacion; }
    public OffsetDateTime getFechaCierre() { return fechaCierre; }
}
