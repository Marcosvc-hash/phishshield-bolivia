package bo.phishshield.backend.empresa.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public class Empresa {

    private static final String PLANES = "basico|profesional|empresarial";
    /** Igual que chk_empresas_nit: entre 7 y 15 digitos. */
    private static final String FORMATO_NIT = "[0-9]{7,15}";

    private final Long id;
    private final UUID uuidPublico;
    private String nombre;
    private String nit;
    private String sector;
    private String ciudad;
    private String plan;
    private boolean activa;
    private final OffsetDateTime creadoEn;

    public Empresa(Long id, UUID uuidPublico, String nombre, String nit,
                   String sector, String ciudad, String plan,
                   boolean activa, OffsetDateTime creadoEn) {
        this.id = id;
        this.uuidPublico = uuidPublico;
        this.activa = activa;
        this.creadoEn = creadoEn;
        asignarDatos(nombre, nit, sector, ciudad, plan);
    }

    public static Empresa nueva(String nombre, String nit, String sector,
                                String ciudad, String plan) {
        return new Empresa(
                null,
                null,
                nombre,
                nit,
                sector,
                ciudad,
                plan == null || plan.isBlank() ? "basico" : plan,
                true,
                null
        );
    }

    /** Corrige los datos de la empresa con las mismas reglas que al crearla. */
    public void actualizarDatos(String nombre, String nit, String sector, String ciudad, String plan) {
        asignarDatos(nombre, nit, sector, ciudad, plan == null || plan.isBlank() ? this.plan : plan);
    }

    /**
     * Una empresa inactiva sigue existiendo con su historial, pero no se le
     * pueden crear usuarios nuevos (ver PoliticaGestionUsuarios). Sus
     * usuarios actuales siguen entrando: bloquearlos es otra decision
     * (se desactivan uno por uno desde Usuarios).
     */
    public void desactivar() {
        this.activa = false;
    }

    public void activar() {
        this.activa = true;
    }

    private void asignarDatos(String nombre, String nit, String sector, String ciudad, String plan) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la empresa es obligatorio");
        }
        String nitLimpio = opcional(nit);
        if (nitLimpio != null && !nitLimpio.matches(FORMATO_NIT)) {
            throw new IllegalArgumentException("El NIT debe tener entre 7 y 15 digitos");
        }
        if (plan != null && !plan.matches(PLANES)) {
            throw new IllegalArgumentException("Plan invalido: " + plan);
        }
        this.nombre = nombre.strip();
        this.nit = nitLimpio;
        this.sector = opcional(sector);
        this.ciudad = opcional(ciudad);
        this.plan = plan;
    }

    /** Un texto vacio se guarda como null (el NIT vacio violaria chk_empresas_nit). */
    private static String opcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }

    public Long getId() { return id; }
    public UUID getUuidPublico() { return uuidPublico; }
    public String getNombre() { return nombre; }
    public String getNit() { return nit; }
    public String getSector() { return sector; }
    public String getCiudad() { return ciudad; }
    public String getPlan() { return plan; }
    public boolean isActiva() { return activa; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
}
