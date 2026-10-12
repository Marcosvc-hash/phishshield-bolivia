package bo.phishshield.backend.analisis.infrastructure.persistence;

import bo.phishshield.backend.analisis.domain.model.Alerta;
import bo.phishshield.backend.analisis.domain.model.Analisis;
import bo.phishshield.backend.analisis.domain.model.Indicador;
import bo.phishshield.backend.analisis.domain.model.OrigenAnalisis;
import bo.phishshield.backend.analisis.domain.model.TipoAnalisis;
import bo.phishshield.backend.analisis.domain.port.out.AnalisisRepositoryPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de salida: traduce entre el dominio y las tablas
 * analisis / analisis_indicadores / alertas.
 */
@Component
public class AnalisisPersistenceAdapter implements AnalisisRepositoryPort {

    private static final String ESTADO_ALERTA_INICIAL = "pendiente";

    private final AnalisisJpaRepository analisisRepository;
    private final AnalisisIndicadorJpaRepository indicadorRepository;
    private final ReglaDeteccionJpaRepository reglaRepository;
    private final AlertaJpaRepository alertaRepository;

    public AnalisisPersistenceAdapter(AnalisisJpaRepository analisisRepository,
                                      AnalisisIndicadorJpaRepository indicadorRepository,
                                      ReglaDeteccionJpaRepository reglaRepository,
                                      AlertaJpaRepository alertaRepository) {
        this.analisisRepository = analisisRepository;
        this.indicadorRepository = indicadorRepository;
        this.reglaRepository = reglaRepository;
        this.alertaRepository = alertaRepository;
    }

    /**
     * Escribe en dos tablas. Si falla la segunda, la primera se revierte:
     * no queremos analisis sin indicadores ni indicadores huerfanos.
     */
    @Override
    @Transactional
    public Analisis guardar(Analisis analisis) {
        AnalisisEntity entidad = aEntidad(analisis);
        AnalisisEntity guardada = analisisRepository.save(entidad);

        Map<String, Long> idsPorCodigo = idsDeReglas();

        for (Indicador indicador : analisis.getIndicadores()) {
            Long idRegla = idsPorCodigo.get(indicador.codigoRegla());
            if (idRegla == null) {
                // La regla existe en Java pero no en la base: se omite el
                // indicador en vez de romper el analisis completo.
                continue;
            }
            AnalisisIndicadorEntity fila = new AnalisisIndicadorEntity();
            fila.setIdAnalisis(guardada.getIdAnalisis());
            fila.setIdRegla(idRegla);
            fila.setValor(indicador.valor());
            fila.setAporte(indicador.aporte());
            indicadorRepository.save(fila);
        }

        analisis.asignarIdentidad(
                guardada.getIdAnalisis(),
                guardada.getUuidPublico(),
                guardada.getFechaAnalisis());

        return analisis;
    }

    /**
     * Tres tablas en una transaccion. La llamada interna a guardar() no abre
     * otra transaccion (es una llamada dentro del mismo objeto): todo queda
     * bajo la de este metodo.
     */
    @Override
    @Transactional
    public Analisis guardarConAlerta(Analisis analisis, Alerta alerta) {
        Analisis guardado = guardar(analisis);

        AlertaEntity fila = new AlertaEntity();
        fila.setIdAnalisis(guardado.getId());
        fila.setSeveridad(alerta.severidad().aValorBd());
        fila.setEstado(ESTADO_ALERTA_INICIAL);
        fila.setCanal(alerta.canal().aValorBd());
        alertaRepository.save(fila);

        return guardado;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Analisis> buscarPorHash(String contenidoHash) {
        return analisisRepository
                .findFirstByContenidoHashOrderByFechaAnalisisDesc(contenidoHash)
                .map(this::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Analisis> buscarPorUuid(UUID uuidPublico) {
        return analisisRepository.findByUuidPublico(uuidPublico).map(this::aDominio);
    }

    /**
     * Tres consultas en total, sin importar el limite: los analisis, todos
     * sus indicadores de una vez y el catalogo de reglas. Antes eran dos
     * consultas extra por cada fila (N+1).
     */
    @Override
    @Transactional(readOnly = true)
    public List<Analisis> listarUltimos(int limite) {
        return aDominio(analisisRepository
                .findAllByOrderByFechaAnalisisDesc(PageRequest.of(0, Math.max(1, limite))));
    }

    // ---- Traduccion ----

    private AnalisisEntity aEntidad(Analisis analisis) {
        AnalisisEntity entidad = new AnalisisEntity();
        entidad.setUuidPublico(
                analisis.getUuidPublico() != null ? analisis.getUuidPublico() : UUID.randomUUID());
        entidad.setIdUsuario(analisis.getIdUsuario());
        entidad.setIdEmpresa(analisis.getIdEmpresa());
        entidad.setIdModelo(analisis.getIdModelo());
        entidad.setTipo(analisis.getTipo().aValorBd());
        entidad.setOrigen(analisis.getOrigen().aValorBd());
        entidad.setContenido(analisis.getContenido());
        entidad.setContenidoHash(analisis.getContenidoHash());
        entidad.setDominio(analisis.getDominio());
        entidad.setResultado(analisis.getResultado().aValorBd());
        entidad.setNivelRiesgo(analisis.getNivelRiesgo());
        entidad.setTiempoMs(analisis.getTiempoMs());
        entidad.setFechaAnalisis(
                analisis.getFechaAnalisis() != null
                        ? analisis.getFechaAnalisis()
                        : OffsetDateTime.now());
        return entidad;
    }

    private Analisis aDominio(AnalisisEntity entidad) {
        return aDominio(List.of(entidad)).get(0);
    }

    /** Traduce un lote de filas cargando indicadores y reglas una sola vez. */
    private List<Analisis> aDominio(List<AnalisisEntity> entidades) {
        if (entidades.isEmpty()) {
            return List.of();
        }

        List<Long> ids = entidades.stream().map(AnalisisEntity::getIdAnalisis).toList();
        Map<Long, String> codigosPorId = codigosDeReglas();
        Map<Long, List<Indicador>> indicadoresPorAnalisis = new HashMap<>();

        for (AnalisisIndicadorEntity fila : indicadorRepository.findByIdAnalisisIn(ids)) {
            String codigo = codigosPorId.get(fila.getIdRegla());
            if (codigo != null) {
                indicadoresPorAnalisis
                        .computeIfAbsent(fila.getIdAnalisis(), id -> new ArrayList<>())
                        .add(new Indicador(codigo, fila.getValor(), fila.getAporte()));
            }
        }

        return entidades.stream()
                .map(e -> aDominio(e, indicadoresPorAnalisis.getOrDefault(e.getIdAnalisis(), List.of())))
                .toList();
    }

    private Analisis aDominio(AnalisisEntity entidad, List<Indicador> indicadores) {
        return Analisis.desdePersistencia(
                entidad.getIdAnalisis(),
                entidad.getUuidPublico(),
                entidad.getIdUsuario(),
                entidad.getIdEmpresa(),
                entidad.getIdModelo(),
                TipoAnalisis.desdeValorBd(entidad.getTipo()),
                OrigenAnalisis.desdeValorBd(entidad.getOrigen()),
                entidad.getContenido(),
                entidad.getContenidoHash().trim(),
                entidad.getDominio(),
                entidad.getNivelRiesgo(),
                entidad.getTiempoMs(),
                entidad.getFechaAnalisis(),
                indicadores);
    }

    private Map<String, Long> idsDeReglas() {
        Map<String, Long> mapa = new HashMap<>();
        for (ReglaDeteccionEntity regla : reglaRepository.findAll()) {
            mapa.put(regla.getCodigo(), regla.getIdRegla());
        }
        return mapa;
    }

    private Map<Long, String> codigosDeReglas() {
        Map<Long, String> mapa = new HashMap<>();
        for (ReglaDeteccionEntity regla : reglaRepository.findAll()) {
            mapa.put(regla.getIdRegla(), regla.getCodigo());
        }
        return mapa;
    }
}