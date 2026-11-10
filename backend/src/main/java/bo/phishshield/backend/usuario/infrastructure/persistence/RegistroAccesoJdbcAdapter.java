package bo.phishshield.backend.usuario.infrastructure.persistence;

import bo.phishshield.backend.usuario.domain.model.EventoAcceso;
import bo.phishshield.backend.usuario.domain.port.out.RegistroAccesoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Escribe los accesos en la tabla auditoria con JdbcClient (SQL directo).
 * Se usa SQL y no una entidad JPA por las columnas JSONB e INET, que
 * necesitan un CAST explicito en PostgreSQL.
 */
@Component
public class RegistroAccesoJdbcAdapter implements RegistroAccesoPort {

    private static final Logger log = LoggerFactory.getLogger(RegistroAccesoJdbcAdapter.class);

    private final JdbcClient jdbc;
    private final JsonMapper json;

    public RegistroAccesoJdbcAdapter(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /**
     * Si falla la escritura del registro, se avisa en el log pero no se
     * interrumpe el login: perder un registro es menos grave que dejar
     * a todos sin poder entrar.
     */
    @Override
    public void registrar(EventoAcceso evento) {
        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("email", evento.email());
        if (evento.motivo() != null) {
            detalle.put("motivo", evento.motivo());
        }
        if (evento.userAgent() != null) {
            detalle.put("user_agent", evento.userAgent());
        }

        try {
            jdbc.sql("""
                    INSERT INTO auditoria (id_usuario, accion, entidad, id_entidad_afectada, detalle, ip_origen)
                    VALUES (CAST(:idUsuario AS BIGINT), :accion, 'usuarios', CAST(:idUsuario AS BIGINT),
                            CAST(:detalle AS JSONB), CAST(:ip AS INET))
                    """)
                    .param("idUsuario", evento.idUsuario())
                    .param("accion", evento.tipo().name())
                    .param("detalle", json.writeValueAsString(detalle))
                    .param("ip", evento.ip())
                    .update();
        } catch (RuntimeException ex) {
            log.warn("No se pudo registrar el acceso {} de {}: {}",
                    evento.tipo(), evento.email(), ex.getMessage());
        }
    }
}
