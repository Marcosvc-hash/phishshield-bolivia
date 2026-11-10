package bo.phishshield.backend.analisis.infrastructure.persistence;

import bo.phishshield.backend.analisis.domain.port.out.CambiosListasPort;
import bo.phishshield.backend.shared.domain.model.AccionAuditoria;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * La fecha del ultimo cambio de listas sale de la tabla auditoria: cada
 * alta, edicion, baja o reactivacion de un dominio deja ahi una fila
 * DOMINIO_*. Asi no hace falta una columna nueva en dominios_lista.
 *
 * Limite conocido: si la escritura en auditoria fallara (solo deja un WARN),
 * ese cambio no invalidaria la cache y regiria el plazo normal de 24 h.
 */
@Component
public class CambiosListasJdbcAdapter implements CambiosListasPort {

    private static final List<String> ACCIONES_DE_LISTAS = Arrays.stream(AccionAuditoria.values())
            .filter(AccionAuditoria::cambiaListasDeDominios)
            .map(Enum::name)
            .toList();

    private final JdbcClient jdbc;

    public CambiosListasJdbcAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<OffsetDateTime> ultimoCambio() {
        // MAX() sin filas devuelve una fila con NULL: optional() la convierte en vacio
        return jdbc.sql("SELECT MAX(creado_en) FROM auditoria WHERE accion IN (:acciones)")
                .param("acciones", ACCIONES_DE_LISTAS)
                .query(OffsetDateTime.class)
                .optional();
    }
}
