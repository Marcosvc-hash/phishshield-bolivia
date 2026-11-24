package bo.phishshield.backend.qa.application;

import bo.phishshield.backend.qa.domain.model.Suite;

import java.util.List;
import java.util.Map;

/**
 * Lo que se pide al pulsar "Ejecutar" en el Centro de Pruebas.
 *
 * @param suite      la suite completa a ejecutar; puede venir null si se mandan codigos
 * @param codigos    casos marcados uno por uno; vacio = la suite completa
 * @param correo     destinatario de los reportes; vacio = el correo de quien la lanza
 * @param parametros ajustes de la ejecucion (usuarios concurrentes, umbral de p95...)
 */
public record ComandoIniciarEjecucion(Suite suite, List<String> codigos, String correo,
                                      Map<String, String> parametros) {

    public ComandoIniciarEjecucion {
        codigos = codigos == null ? List.of() : List.copyOf(codigos);
        parametros = parametros == null ? Map.of() : Map.copyOf(parametros);
    }

    public boolean porCodigos() {
        return !codigos.isEmpty();
    }
}
