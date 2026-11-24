package bo.phishshield.backend.qa.infrastructure.persistence;

import bo.phishshield.backend.qa.domain.model.AlcanceEjecucion;
import bo.phishshield.backend.qa.domain.model.CasoPrueba;
import bo.phishshield.backend.qa.domain.model.CorreoDestino;
import bo.phishshield.backend.qa.domain.model.Ejecucion;
import bo.phishshield.backend.qa.domain.model.EjecucionEnCursoException;
import bo.phishshield.backend.qa.domain.model.EstadoCaso;
import bo.phishshield.backend.qa.domain.model.EstadoCorreo;
import bo.phishshield.backend.qa.domain.model.EstadoEjecucion;
import bo.phishshield.backend.qa.domain.model.EstadoPaso;
import bo.phishshield.backend.qa.domain.model.FilaEjecucion;
import bo.phishshield.backend.qa.domain.model.MetricaRendimiento;
import bo.phishshield.backend.qa.domain.model.PasoEjecutado;
import bo.phishshield.backend.qa.domain.model.ResultadoCaso;
import bo.phishshield.backend.qa.domain.model.ResultadoGlobal;
import bo.phishshield.backend.qa.domain.model.ResumenEjecucion;
import bo.phishshield.backend.qa.domain.model.Suite;
import bo.phishshield.backend.qa.domain.model.TipoFallo;
import bo.phishshield.backend.qa.domain.model.TransicionEjecucionInvalidaException;
import bo.phishshield.backend.qa.domain.port.out.EjecucionRepositoryPort;
import bo.phishshield.backend.shared.domain.model.Pagina;
import bo.phishshield.backend.shared.domain.model.SolicitudPagina;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Ejecuciones del Centro de Pruebas con sus resultados, pasos y metricas,
 * con SQL directo (JdbcClient), al estilo de los demas *JdbcAdapter.
 *
 * Reglas al guardar:
 *  - Cada guardado es una transaccion.
 *  - Una ejecucion que ya termino en la base no vuelve a estar activa: el
 *    UPDATE solo pasa si la fila sigue activa o ya tiene el estado que se
 *    escribe (asi, detenerla "a mano" no se pisa con un guardado tardio).
 *  - Los pasos y las metricas se insertan una sola vez y nunca se borran.
 */
@Component
public class EjecucionJdbcAdapter implements EjecucionRepositoryPort {

    private static final TypeReference<Map<String, Object>> MAPA = new TypeReference<>() { };

    private static final String COLUMNAS_EJECUCION = """
            e.id_ejecucion, e.uuid_publico, e.id_usuario, e.suite, e.alcance, e.correo_destino, e.ambiente,
            e.parametros::text AS parametros, e.estado, e.resultado_global, e.creado_en, e.inicio, e.fin,
            e.mensaje_error, e.estado_correo, e.error_correo, e.correo_enviado_en,
            e.ruta_reporte_pdf, e.ruta_reporte_html
            """;

    private final JdbcClient jdbc;
    private final TransactionTemplate transaccion;
    private final JsonMapper json;

    public EjecucionJdbcAdapter(JdbcClient jdbc, TransactionTemplate transaccion, JsonMapper json) {
        this.jdbc = jdbc;
        this.transaccion = transaccion;
        this.json = json;
    }

    // ------------------------------------------------------------------
    // Escritura
    // ------------------------------------------------------------------

    @Override
    public Ejecucion guardar(Ejecucion ejecucion) {
        if (ejecucion.getId() == null) {
            try {
                transaccion.executeWithoutResult(t -> insertar(ejecucion));
            } catch (DuplicateKeyException ex) {
                // uq_qa_ejec_activa: otra peticion lanzo una ejecucion al mismo tiempo
                throw new EjecucionEnCursoException(buscarActiva().orElse(null));
            }
            return buscarPorUuid(ejecucion.getUuidPublico()).orElseThrow();
        }
        transaccion.executeWithoutResult(t -> actualizar(ejecucion));
        return ejecucion;
    }

    private void insertar(Ejecucion e) {
        long id = jdbc.sql("""
                        INSERT INTO qa_ejecuciones (uuid_publico, id_usuario, tipo, suite, alcance, correo_destino,
                                                    ambiente, parametros, estado, resultado_global, creado_en,
                                                    inicio, fin, mensaje_error, estado_correo)
                        VALUES (:uuid, :usuario, :tipo, :suite, :alcance, :correo, :ambiente, CAST(:parametros AS JSONB),
                                :estado, :resultado, :creado, :inicio, :fin, :error, :estadoCorreo)
                        RETURNING id_ejecucion
                        """)
                .param("uuid", e.getUuidPublico())
                .param("usuario", e.getIdUsuario())
                .param("tipo", e.tipo().aValorBd())
                .param("suite", e.getSuite().aValorBd())
                .param("alcance", e.getAlcance().aValorBd())
                .param("correo", e.getCorreo().valor())
                .param("ambiente", e.getAmbiente())
                .param("parametros", json.writeValueAsString(e.getParametros()))
                .param("estado", e.getEstado().aValorBd())
                .param("resultado", e.getResultadoGlobal() == null ? null : e.getResultadoGlobal().aValorBd())
                .param("creado", e.getCreadaEn())
                .param("inicio", e.getInicio())
                .param("fin", e.getFin())
                .param("error", e.getMensajeError())
                .param("estadoCorreo", e.getEstadoCorreo().aValorBd())
                .query(Long.class)
                .single();

        for (ResultadoCaso r : e.getResultados()) {
            jdbc.sql("""
                            INSERT INTO qa_resultados (id_ejecucion, id_caso, orden, estado, observacion)
                            VALUES (:ejecucion, :caso, :orden, :estado, :observacion)
                            """)
                    .param("ejecucion", id)
                    .param("caso", r.getCaso().id())
                    .param("orden", r.getOrden())
                    .param("estado", r.getEstado().aValorBd())
                    .param("observacion", r.getObservacion())
                    .update();
        }
    }

    private void actualizar(Ejecucion e) {
        int filas = jdbc.sql("""
                        UPDATE qa_ejecuciones
                        SET estado = :estado, resultado_global = :resultado, inicio = :inicio, fin = :fin,
                            mensaje_error = :error, estado_correo = :estadoCorreo, error_correo = :errorCorreo,
                            correo_enviado_en = :correoEnviado, ruta_reporte_pdf = :pdf, ruta_reporte_html = :html
                        WHERE id_ejecucion = :id
                          AND (estado IN ('pendiente', 'en_curso') OR estado = :estado)
                        """)
                .param("estado", e.getEstado().aValorBd())
                .param("resultado", e.getResultadoGlobal() == null ? null : e.getResultadoGlobal().aValorBd())
                .param("inicio", e.getInicio())
                .param("fin", e.getFin())
                .param("error", e.getMensajeError())
                .param("estadoCorreo", e.getEstadoCorreo().aValorBd())
                .param("errorCorreo", e.getErrorCorreo())
                .param("correoEnviado", e.getCorreoEnviadoEn())
                .param("pdf", e.getRutaReportePdf())
                .param("html", e.getRutaReporteHtml())
                .param("id", e.getId())
                .update();
        if (filas == 0) {
            throw new TransicionEjecucionInvalidaException(
                    "La ejecución " + e.getUuidPublico() + " ya terminó en la base; no se sobrescribe");
        }

        Set<Long> conPasos = idsConDetalle("qa_pasos", e.getId());
        Set<Long> conMetricas = idsConDetalle("qa_metricas", e.getId());

        for (ResultadoCaso r : e.getResultados()) {
            jdbc.sql("""
                            UPDATE qa_resultados
                            SET estado = :estado, tipo_fallo = :tipoFallo, mensaje_error = :error,
                                paso_fallido = :pasoFallido, observacion = :observacion, inicio = :inicio, fin = :fin
                            WHERE id_resultado = :id
                            """)
                    .param("estado", r.getEstado().aValorBd())
                    .param("tipoFallo", r.getTipoFallo() == null ? null : r.getTipoFallo().aValorBd())
                    .param("error", r.getMensajeError())
                    .param("pasoFallido", r.getPasoFallido())
                    .param("observacion", r.getObservacion())
                    .param("inicio", r.getInicio())
                    .param("fin", r.getFin())
                    .param("id", r.getId())
                    .update();

            if (!conPasos.contains(r.getId())) {
                r.getPasos().forEach(p -> insertarPaso(r.getId(), p));
            }
            if (!conMetricas.contains(r.getId())) {
                List<MetricaRendimiento> metricas = r.getMetricas();
                for (int i = 0; i < metricas.size(); i++) {
                    insertarMetrica(r.getId(), i + 1, metricas.get(i));
                }
            }
        }
    }

    private Set<Long> idsConDetalle(String tabla, long idEjecucion) {
        return new HashSet<>(jdbc.sql("""
                        SELECT DISTINCT d.id_resultado
                        FROM %s d JOIN qa_resultados r ON r.id_resultado = d.id_resultado
                        WHERE r.id_ejecucion = :id
                        """.formatted(tabla))
                .param("id", idEjecucion)
                .query(Long.class)
                .list());
    }

    private void insertarPaso(long idResultado, PasoEjecutado p) {
        jdbc.sql("""
                        INSERT INTO qa_pasos (id_resultado, numero, descripcion, resultado_observado, estado,
                                              ruta_captura, detalle_api, registrado_en)
                        VALUES (:resultado, :numero, :descripcion, :observado, :estado, :captura, :api, :registrado)
                        ON CONFLICT (id_resultado, numero) DO NOTHING
                        """)
                .param("resultado", idResultado)
                .param("numero", p.numero())
                .param("descripcion", p.descripcion())
                .param("observado", p.resultadoObservado())
                .param("estado", p.estado().aValorBd())
                .param("captura", p.rutaCaptura())
                .param("api", p.detalleApi())
                .param("registrado", p.registradoEn())
                .update();
    }

    private void insertarMetrica(long idResultado, int orden, MetricaRendimiento m) {
        jdbc.sql("""
                        INSERT INTO qa_metricas (id_resultado, orden, nombre, valor, unidad, umbral, cumple, escalon)
                        VALUES (:resultado, :orden, :nombre, :valor, :unidad, :umbral, :cumple, :escalon)
                        ON CONFLICT ON CONSTRAINT uq_qa_metricas DO NOTHING
                        """)
                .param("resultado", idResultado)
                .param("orden", orden)
                .param("nombre", m.nombre())
                .param("valor", m.valor())
                .param("unidad", m.unidad())
                .param("umbral", m.umbral())
                .param("cumple", m.cumple())
                .param("escalon", m.escalon())
                .update();
    }

    // ------------------------------------------------------------------
    // Lectura
    // ------------------------------------------------------------------

    @Override
    public Optional<Ejecucion> buscarPorUuid(UUID uuidPublico) {
        return jdbc.sql("SELECT " + COLUMNAS_EJECUCION + " FROM qa_ejecuciones e WHERE e.uuid_publico = :uuid")
                .param("uuid", uuidPublico)
                .query((rs, n) -> new FilaLeida(
                        rs.getLong("id_ejecucion"),
                        rs.getObject("uuid_publico", UUID.class),
                        rs.getObject("id_usuario", Long.class),
                        rs.getString("suite"), rs.getString("alcance"), rs.getString("correo_destino"),
                        rs.getString("ambiente"), rs.getString("parametros"), rs.getString("estado"),
                        rs.getString("resultado_global"),
                        rs.getObject("creado_en", OffsetDateTime.class),
                        rs.getObject("inicio", OffsetDateTime.class),
                        rs.getObject("fin", OffsetDateTime.class),
                        rs.getString("mensaje_error"), rs.getString("estado_correo"), rs.getString("error_correo"),
                        rs.getObject("correo_enviado_en", OffsetDateTime.class),
                        rs.getString("ruta_reporte_pdf"), rs.getString("ruta_reporte_html")))
                .optional()
                .map(this::armar);
    }

    private Ejecucion armar(FilaLeida f) {
        Map<Long, List<PasoEjecutado>> pasos = jdbc.sql("""
                        SELECT p.id_resultado, p.numero, p.descripcion, p.resultado_observado, p.estado,
                               p.ruta_captura, p.detalle_api, p.registrado_en
                        FROM qa_pasos p JOIN qa_resultados r ON r.id_resultado = p.id_resultado
                        WHERE r.id_ejecucion = :id
                        ORDER BY p.id_resultado, p.numero
                        """)
                .param("id", f.id())
                .query((rs, n) -> Map.entry(rs.getLong("id_resultado"), new PasoEjecutado(
                        rs.getInt("numero"),
                        rs.getString("descripcion"),
                        rs.getString("resultado_observado"),
                        EstadoPaso.desdeValorBd(rs.getString("estado")),
                        rs.getString("ruta_captura"),
                        rs.getString("detalle_api"),
                        rs.getObject("registrado_en", OffsetDateTime.class))))
                .list()
                .stream()
                .collect(Collectors.groupingBy(Map.Entry::getKey, LinkedHashMap::new,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        Map<Long, List<MetricaRendimiento>> metricas = jdbc.sql("""
                        SELECT m.id_resultado, m.nombre, m.valor, m.unidad, m.umbral, m.cumple, m.escalon
                        FROM qa_metricas m JOIN qa_resultados r ON r.id_resultado = m.id_resultado
                        WHERE r.id_ejecucion = :id
                        ORDER BY m.id_resultado, m.escalon NULLS FIRST, m.orden, m.id_metrica
                        """)
                .param("id", f.id())
                .query((rs, n) -> Map.entry(rs.getLong("id_resultado"), new MetricaRendimiento(
                        rs.getString("nombre"),
                        rs.getBigDecimal("valor"),
                        rs.getString("unidad"),
                        rs.getBigDecimal("umbral"),
                        rs.getObject("cumple", Boolean.class),
                        rs.getObject("escalon", Integer.class))))
                .list()
                .stream()
                .collect(Collectors.groupingBy(Map.Entry::getKey, LinkedHashMap::new,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        List<ResultadoCaso> resultados = jdbc.sql("""
                        SELECT r.id_resultado, r.orden AS orden_resultado, r.estado AS estado_resultado, r.tipo_fallo,
                               r.mensaje_error, r.paso_fallido, r.observacion, r.inicio, r.fin,
                        """ + CatalogoCasosJdbcAdapter.COLUMNAS + """
                        FROM qa_resultados r JOIN qa_casos c ON c.id_caso = r.id_caso
                        WHERE r.id_ejecucion = :id
                        ORDER BY r.orden
                        """)
                .param("id", f.id())
                .query((rs, n) -> {
                    long idResultado = rs.getLong("id_resultado");
                    CasoPrueba caso = CatalogoCasosJdbcAdapter.A_CASO.mapRow(rs, n);
                    String tipoFallo = rs.getString("tipo_fallo");
                    return new ResultadoCaso(
                            idResultado,
                            caso,
                            rs.getInt("orden_resultado"),
                            EstadoCaso.desdeValorBd(rs.getString("estado_resultado")),
                            tipoFallo == null ? null : TipoFallo.desdeValorBd(tipoFallo),
                            rs.getString("mensaje_error"),
                            rs.getString("paso_fallido"),
                            rs.getString("observacion"),
                            rs.getObject("inicio", OffsetDateTime.class),
                            rs.getObject("fin", OffsetDateTime.class),
                            pasos.getOrDefault(idResultado, List.of()),
                            metricas.getOrDefault(idResultado, List.of()));
                })
                .list();

        return new Ejecucion(
                f.id(), f.uuid(), f.idUsuario(),
                Suite.desdeValorBd(f.suite()),
                AlcanceEjecucion.desdeValorBd(f.alcance()),
                new CorreoDestino(f.correo()),
                f.ambiente(),
                aParametros(f.parametros()),
                EstadoEjecucion.desdeValorBd(f.estado()),
                f.resultadoGlobal() == null ? null : ResultadoGlobal.desdeValorBd(f.resultadoGlobal()),
                f.creadaEn(), f.inicio(), f.fin(), f.mensajeError(),
                EstadoCorreo.desdeValorBd(f.estadoCorreo()), f.errorCorreo(), f.correoEnviadoEn(),
                f.rutaPdf(), f.rutaHtml(),
                resultados);
    }

    private Map<String, String> aParametros(String texto) {
        if (texto == null || texto.isBlank()) {
            return Map.of();
        }
        Map<String, String> parametros = new LinkedHashMap<>();
        json.readValue(texto, MAPA).forEach((k, v) -> parametros.put(k, v == null ? "" : String.valueOf(v)));
        return parametros;
    }

    @Override
    public Pagina<FilaEjecucion> listar(SolicitudPagina solicitud) {
        long total = jdbc.sql("SELECT COUNT(*) FROM qa_ejecuciones").query(Long.class).single();

        List<FilaEjecucion> filas = jdbc.sql("""
                        SELECT e.uuid_publico, e.suite, e.alcance, e.correo_destino, e.estado, e.resultado_global,
                               e.creado_en, e.inicio, e.fin, e.estado_correo,
                               COUNT(r.id_resultado)                                AS total,
                               COUNT(r.id_resultado) FILTER (WHERE r.estado = 'pass')    AS pass,
                               COUNT(r.id_resultado) FILTER (WHERE r.estado = 'fail')    AS fail,
                               COUNT(r.id_resultado) FILTER (WHERE r.estado = 'skipped') AS skipped,
                               COUNT(r.id_resultado) FILTER (WHERE r.estado = 'not_run') AS not_run
                        FROM qa_ejecuciones e
                        LEFT JOIN qa_resultados r ON r.id_ejecucion = e.id_ejecucion
                        GROUP BY e.id_ejecucion
                        ORDER BY e.creado_en DESC, e.id_ejecucion DESC
                        LIMIT :limite OFFSET :salto
                        """)
                .param("limite", solicitud.tamano())
                .param("salto", solicitud.desplazamiento())
                .query((rs, n) -> {
                    String resultado = rs.getString("resultado_global");
                    return new FilaEjecucion(
                            rs.getObject("uuid_publico", UUID.class),
                            Suite.desdeValorBd(rs.getString("suite")),
                            AlcanceEjecucion.desdeValorBd(rs.getString("alcance")),
                            rs.getString("correo_destino"),
                            EstadoEjecucion.desdeValorBd(rs.getString("estado")),
                            resultado == null ? null : ResultadoGlobal.desdeValorBd(resultado),
                            rs.getObject("creado_en", OffsetDateTime.class),
                            rs.getObject("inicio", OffsetDateTime.class),
                            rs.getObject("fin", OffsetDateTime.class),
                            new ResumenEjecucion(rs.getInt("total"), rs.getInt("pass"), rs.getInt("fail"),
                                    rs.getInt("skipped"), rs.getInt("not_run")),
                            EstadoCorreo.desdeValorBd(rs.getString("estado_correo")));
                })
                .list();

        return Pagina.de(filas, solicitud, total);
    }

    @Override
    public Optional<UUID> buscarActiva() {
        return jdbc.sql("""
                        SELECT uuid_publico FROM qa_ejecuciones
                        WHERE estado IN ('pendiente', 'en_curso')
                        ORDER BY creado_en LIMIT 1
                        """)
                .query(UUID.class)
                .optional();
    }

    @Override
    public List<Ejecucion> listarActivas() {
        List<Ejecucion> activas = new ArrayList<>();
        jdbc.sql("SELECT uuid_publico FROM qa_ejecuciones WHERE estado IN ('pendiente', 'en_curso') ORDER BY creado_en")
                .query(UUID.class)
                .list()
                .forEach(uuid -> buscarPorUuid(uuid).ifPresent(activas::add));
        return activas;
    }

    @Override
    public boolean marcarFallida(UUID uuidPublico, String motivo, OffsetDateTime fin) {
        String texto = motivo == null ? "La ejecución terminó sin completar" : motivo.replace("\u0000", "");
        return jdbc.sql("""
                        UPDATE qa_ejecuciones
                        SET estado = 'fallida', resultado_global = NULL, fin = GREATEST(:fin, COALESCE(inicio, :fin)),
                            mensaje_error = LEFT(:motivo, 4000)
                        WHERE uuid_publico = :uuid AND estado IN ('pendiente', 'en_curso')
                        """)
                .param("fin", fin)
                .param("motivo", texto)
                .param("uuid", uuidPublico)
                .update() == 1;
    }

    /** Una fila de qa_ejecuciones tal como sale de la base, antes de armar el agregado. */
    private record FilaLeida(long id, UUID uuid, Long idUsuario, String suite, String alcance, String correo,
                             String ambiente, String parametros, String estado, String resultadoGlobal,
                             OffsetDateTime creadaEn, OffsetDateTime inicio, OffsetDateTime fin,
                             String mensajeError, String estadoCorreo, String errorCorreo,
                             OffsetDateTime correoEnviadoEn, String rutaPdf, String rutaHtml) {
    }
}
