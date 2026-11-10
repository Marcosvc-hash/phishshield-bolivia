package bo.phishshield.backend.shared.infrastructure.persistence;

import bo.phishshield.backend.shared.domain.model.EventoAuditoria;
import bo.phishshield.backend.shared.domain.port.out.AuditoriaPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Escribe las acciones administrativas en auditoria con SQL directo, igual
 * que RegistroAccesoJdbcAdapter (las columnas JSONB e INET necesitan CAST).
 */
@Component
public class AuditoriaJdbcAdapter implements AuditoriaPort {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaJdbcAdapter.class);

    private final JdbcClient jdbc;
    private final JsonMapper json;

    public AuditoriaJdbcAdapter(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /**
     * Igual que con los logins: si falla la escritura del registro se avisa
     * en el log y la accion ya hecha no se deshace. Para que fueran una sola
     * transaccion, la capa application tendria que manejar transacciones de
     * Spring, y esa capa no lleva Spring.
     */
    @Override
    public void registrar(EventoAuditoria evento) {
        try {
            jdbc.sql("""
                    INSERT INTO auditoria (id_usuario, accion, entidad, id_entidad_afectada, detalle, ip_origen)
                    VALUES (CAST(:idUsuario AS BIGINT), :accion, :entidad, CAST(:idEntidad AS BIGINT),
                            CAST(:detalle AS JSONB), CAST(:ip AS INET))
                    """)
                    .param("idUsuario", evento.idUsuario())
                    .param("accion", evento.accion().name())
                    .param("entidad", evento.entidad())
                    .param("idEntidad", evento.idEntidadAfectada())
                    .param("detalle", json.writeValueAsString(evento.detalle()))
                    .param("ip", evento.ip())
                    .update();
        } catch (RuntimeException ex) {
            log.warn("No se pudo registrar la accion {} del usuario {}: {}",
                    evento.accion(), evento.idUsuario(), ex.getMessage());
        }
    }
}
