package bo.phishshield.backend.listas.infrastructure.persistence;

import bo.phishshield.backend.listas.domain.model.DominioEnLista;
import bo.phishshield.backend.listas.domain.model.DominioFila;
import bo.phishshield.backend.listas.domain.model.FiltroDominios;
import bo.phishshield.backend.listas.domain.model.NombreDominio;
import bo.phishshield.backend.listas.domain.model.TipoLista;
import bo.phishshield.backend.listas.domain.port.out.DominioListaRepositoryPort;
import bo.phishshield.backend.shared.domain.model.Pagina;
import bo.phishshield.backend.shared.domain.model.SolicitudPagina;
import bo.phishshield.backend.shared.infrastructure.persistence.FiltrosSql;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Gestion de dominios_lista con SQL directo. La DominioListaEntity de la
 * vertical analisis sigue siendo de solo lectura para el motor; esta clase
 * es la unica que escribe en la tabla. No hay ningun DELETE: dar de baja
 * es poner activo = false.
 */
@Component
public class DominioListaJdbcAdapter implements DominioListaRepositoryPort {

    private static final String SELECT_FILA = """
            SELECT d.uuid_publico, d.dominio, d.lista, e.nombre AS entidad, d.fuente, d.motivo,
                   d.activo, d.creado_en
            FROM dominios_lista d
            LEFT JOIN entidades_suplantadas e ON e.id_entidad = d.id_entidad
            """;

    private static final String FILTROS = """
            WHERE (CAST(:lista AS VARCHAR) IS NULL OR d.lista = :lista)
              AND (CAST(:activo AS BOOLEAN) IS NULL OR d.activo = :activo)
              AND (CAST(:texto AS VARCHAR) IS NULL
                   OR d.dominio ILIKE :texto ESCAPE '\\'
                   OR d.motivo ILIKE :texto ESCAPE '\\')
            """;

    private static final String SELECT_DOMINIO = """
            SELECT id_dominio, uuid_publico, dominio, lista, fuente, id_entidad, motivo, activo
            FROM dominios_lista
            """;

    private static final RowMapper<DominioFila> A_FILA = (rs, n) -> new DominioFila(
            rs.getObject("uuid_publico", UUID.class),
            rs.getString("dominio"),
            rs.getString("lista"),
            rs.getString("entidad"),
            rs.getString("fuente"),
            rs.getString("motivo"),
            rs.getBoolean("activo"),
            rs.getObject("creado_en", OffsetDateTime.class));

    private static final RowMapper<DominioEnLista> A_DOMINIO = (rs, n) -> new DominioEnLista(
            rs.getLong("id_dominio"),
            rs.getObject("uuid_publico", UUID.class),
            NombreDominio.de(rs.getString("dominio")),
            TipoLista.desdeValorBd(rs.getString("lista")),
            rs.getString("fuente"),
            rs.getObject("id_entidad", Long.class),
            rs.getString("motivo"),
            rs.getBoolean("activo"));

    private final JdbcClient jdbc;

    public DominioListaJdbcAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Activos primero; dentro de cada grupo, por lista y por dominio. */
    @Override
    public Pagina<DominioFila> buscar(FiltroDominios filtro, SolicitudPagina pagina) {
        long total = conFiltros(jdbc.sql("SELECT COUNT(*) FROM dominios_lista d " + FILTROS), filtro)
                .query(Long.class)
                .single();

        List<DominioFila> filas = conFiltros(jdbc.sql(SELECT_FILA + FILTROS + """
                        ORDER BY d.activo DESC, d.lista, d.dominio
                        LIMIT :limite OFFSET :salto
                        """), filtro)
                .param("limite", pagina.tamano())
                .param("salto", pagina.desplazamiento())
                .query(A_FILA)
                .list();

        return Pagina.de(filas, pagina, total);
    }

    @Override
    public Optional<DominioEnLista> buscarPorUuid(UUID id) {
        return jdbc.sql(SELECT_DOMINIO + "WHERE uuid_publico = :id")
                .param("id", id)
                .query(A_DOMINIO)
                .optional();
    }

    @Override
    public Optional<DominioEnLista> buscar(NombreDominio dominio, TipoLista lista) {
        return jdbc.sql(SELECT_DOMINIO + "WHERE dominio = :dominio AND lista = :lista")
                .param("dominio", dominio.valor())
                .param("lista", lista.aValorBd())
                .query(A_DOMINIO)
                .optional();
    }

    @Override
    @Transactional
    public DominioEnLista guardar(DominioEnLista d) {
        if (d.getId() == null) {
            return jdbc.sql("""
                            INSERT INTO dominios_lista (dominio, lista, id_entidad, fuente, motivo, activo)
                            VALUES (:dominio, :lista, :entidad, :fuente, :motivo, :activo)
                            RETURNING id_dominio, uuid_publico, dominio, lista, fuente, id_entidad, motivo, activo
                            """)
                    .param("dominio", d.getDominio().valor())
                    .param("lista", d.getLista().aValorBd())
                    .param("entidad", d.getIdEntidad())
                    .param("fuente", d.getFuente())
                    .param("motivo", d.getMotivo())
                    .param("activo", d.isActivo())
                    .query(A_DOMINIO)
                    .single();
        }

        jdbc.sql("""
                        UPDATE dominios_lista
                        SET motivo = :motivo, id_entidad = :entidad, activo = :activo
                        WHERE id_dominio = :id
                        """)
                .param("motivo", d.getMotivo())
                .param("entidad", d.getIdEntidad())
                .param("activo", d.isActivo())
                .param("id", d.getId())
                .update();
        return d;
    }

    @Override
    public Optional<DominioFila> buscarFila(UUID id) {
        return jdbc.sql(SELECT_FILA + "WHERE d.uuid_publico = :id")
                .param("id", id)
                .query(A_FILA)
                .optional();
    }

    private static JdbcClient.StatementSpec conFiltros(JdbcClient.StatementSpec sql, FiltroDominios filtro) {
        return sql
                .param("lista", filtro.lista() == null ? null : filtro.lista().aValorBd())
                .param("activo", filtro.activo())
                .param("texto", FiltrosSql.contiene(filtro.texto()));
    }
}
