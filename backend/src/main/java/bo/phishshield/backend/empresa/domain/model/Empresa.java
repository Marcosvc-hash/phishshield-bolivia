package bo.phishshield.backend.empresa.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public class Empresa {

    private final Long id;
    private final UUID uuidPublico;
    private final String nombre;
    private final String nit;
    private final String sector;
    private final String ciudad;
    private final String plan;
    private final boolean activa;
    private final OffsetDateTime creadoEn;

    public Empresa(Long id, UUID uuidPublico, String nombre, String nit,
                   String sector, String ciudad, String plan,
                   boolean activa, OffsetDateTime creadoEn) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la empresa es obligatorio");
        }
        if (plan != null && !plan.matches("basico|profesional|empresarial")) {
            throw new IllegalArgumentException("Plan invalido: " + plan);
        }
        this.id = id;
        this.uuidPublico = uuidPublico;
        this.nombre = nombre;
        this.nit = nit;
        this.sector = sector;
        this.ciudad = ciudad;
        this.plan = plan;
        this.activa = activa;
        this.creadoEn = creadoEn;
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
                plan == null ? "basico" : plan,
                true,
                null
        );
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