package bo.phishshield.backend.listas.domain.model;

import java.util.UUID;

/**
 * Una fila de dominios_lista vista por el panel de administracion.
 *
 * "Eliminar" es desactivar: la fila nunca se borra, para conservar el
 * historial (quien la agrego, por que) y poder reactivarla.
 */
public class DominioEnLista {

    public static final int MAXIMO_MOTIVO = 500;
    public static final String FUENTE_PANEL = "manual";

    private final Long id;
    private final UUID uuidPublico;
    private final NombreDominio dominio;
    private final TipoLista lista;
    private final String fuente;
    private Long idEntidad;
    private String motivo;
    private boolean activo;

    public DominioEnLista(Long id, UUID uuidPublico, NombreDominio dominio, TipoLista lista,
                          String fuente, Long idEntidad, String motivo, boolean activo) {
        if (dominio == null || lista == null) {
            throw new IllegalArgumentException("El dominio y la lista son obligatorios");
        }
        this.id = id;
        this.uuidPublico = uuidPublico;
        this.dominio = dominio;
        this.lista = lista;
        this.fuente = fuente;
        this.idEntidad = idEntidad;
        this.motivo = motivo;
        this.activo = activo;
    }

    /**
     * Alta desde el panel. En la lista blanca se exige el dominio registrable
     * (banco.com.bo, no login.banco.com.bo) porque el motor la compara por
     * dominio registrable exacto: un subdominio ahi no tendria efecto.
     */
    public static DominioEnLista nuevo(NombreDominio dominio, TipoLista lista, String motivo, Long idEntidad) {
        if (lista == null || !lista.permiteAltas()) {
            throw new IllegalArgumentException("Solo se pueden agregar dominios a la lista blanca o a la negra");
        }
        if (lista == TipoLista.BLANCA && !dominio.esRegistrable()) {
            throw new IllegalArgumentException("En la lista blanca va el dominio registrable: use "
                    + dominio.registrable() + " en lugar de " + dominio.valor());
        }
        return new DominioEnLista(null, null, dominio, lista, FUENTE_PANEL, idEntidad, motivoValido(motivo), true);
    }

    public void editar(String motivo, Long idEntidad) {
        this.motivo = motivoValido(motivo);
        this.idEntidad = idEntidad;
    }

    public void desactivar() {
        this.activo = false;
    }

    public void reactivar() {
        this.activo = true;
    }

    /** El motivo es obligatorio: dentro de un ano nadie recordara por que se agrego. */
    private static String motivoValido(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Indique el motivo");
        }
        String limpio = motivo.strip();
        if (limpio.length() > MAXIMO_MOTIVO) {
            throw new IllegalArgumentException("El motivo no puede superar los " + MAXIMO_MOTIVO + " caracteres");
        }
        return limpio;
    }

    public Long getId() { return id; }
    public UUID getUuidPublico() { return uuidPublico; }
    public NombreDominio getDominio() { return dominio; }
    public TipoLista getLista() { return lista; }
    public String getFuente() { return fuente; }
    public Long getIdEntidad() { return idEntidad; }
    public String getMotivo() { return motivo; }
    public boolean isActivo() { return activo; }
}
