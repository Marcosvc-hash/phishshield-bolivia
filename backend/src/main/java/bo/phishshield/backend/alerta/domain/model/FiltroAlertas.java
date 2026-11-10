package bo.phishshield.backend.alerta.domain.model;

/** Filtros de la bandeja de alertas; null = sin filtro. */
public record FiltroAlertas(EstadoAlerta estado, SeveridadAlerta severidad) {

    /** Desde los textos de la peticion; los vacios quedan como "sin filtro". */
    public static FiltroAlertas desdeTexto(String estado, String severidad) {
        return new FiltroAlertas(
                estado == null || estado.isBlank() ? null : EstadoAlerta.desdeValorBd(estado),
                severidad == null || severidad.isBlank() ? null : SeveridadAlerta.desdeValorBd(severidad));
    }
}
