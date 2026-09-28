package bo.phishshield.backend.analisis.infrastructure.persistence;

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
 * analisis / analisis_indicadores.
 */
@Component
public class AnalisisPersistenceAdapter implements AnalisisRepositoryPort {

    private final AnalisisJpaRepository analisisRepository;
    private final AnalisisIndicadorJpaRepository indicadorRepository;
    private final ReglaDeteccionJpaRepository reglaRepository;

    public AnalisisPersistenceAdapter(AnalisisJpaRepository analisisRepository,
                                      AnalisisIndicadorJpaRepository indicadorRepository,
                                      ReglaDeteccionJpaRepository reglaRepository) {
        this.analisisRepository = analisisRepository;
        this.indicadorRepository = indicadorRepository;
        this.reglaRepository = reglaRepository;
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

    @Override
    @Transactional(readOnly = true)
    public Optional<Analisis> buscarPorHash(String contenidoHash) {
        return analisisRepository.findByContenidoHash(contenidoHash).map(this::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Analisis> buscarPorUuid(UUID uuidPublico) {
        return analisisRepository.findByUuidPublico(uuidPublico).map(this::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analisis> listarUltimos(int limite) {
        return analisisRepository
                .findAllByOrderByFechaAnalisisDesc(PageRequest.of(0, Math.max(1, limite)))
                .stream()
                .map(this::aDominio)
                .toList();
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
                indicadoresDe(entidad.getIdAnalisis()));
    }

    private List<Indicador> indicadoresDe(Long idAnalisis) {
        Map<Long, String> codigosPorId = codigosDeReglas();
        List<Indicador> indicadores = new ArrayList<>();

        for (AnalisisIndicadorEntity fila : indicadorRepository.findByIdAnalisis(idAnalisis)) {
            String codigo = codigosPorId.get(fila.getIdRegla());
            if (codigo != null) {
                indicadores.add(new Indicador(codigo, fila.getValor(), fila.getAporte()));
            }
        }
        return indicadores;
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