package bo.phishshield.backend.listas.infrastructure.persistence;

import bo.phishshield.backend.listas.domain.model.EntidadSuplantada;
import bo.phishshield.backend.listas.domain.port.out.EntidadSuplantadaPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class EntidadSuplantadaJdbcAdapter implements EntidadSuplantadaPort {

    private final JdbcClient jdbc;

    public EntidadSuplantadaJdbcAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<EntidadSuplantada> listar() {
        return jdbc.sql("SELECT nombre, tipo, dominio_oficial FROM entidades_suplantadas ORDER BY nombre")
                .query((rs, n) -> new EntidadSuplantada(
                        rs.getString("nombre"), rs.getString("tipo"), rs.getString("dominio_oficial")))
                .list();
    }

    @Override
    public Optional<Long> idPorNombre(String nombre) {
        return jdbc.sql("SELECT id_entidad FROM entidades_suplantadas WHERE nombre = :nombre")
                .param("nombre", nombre)
                .query(Long.class)
                .optional();
    }
}
