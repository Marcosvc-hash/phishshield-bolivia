package bo.phishshield.backend.reporte.domain.model;

import java.util.UUID;

/** Lo que el reporte necesita saber de la empresa: su id interno, el publico, el nombre y si esta activa. */
public record EmpresaDelReporte(long id, UUID uuidPublico, String nombre, boolean activa) {

    public EmpresaDelReporte {
        if (uuidPublico == null || nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("La empresa del reporte necesita uuid y nombre");
        }
    }
}
