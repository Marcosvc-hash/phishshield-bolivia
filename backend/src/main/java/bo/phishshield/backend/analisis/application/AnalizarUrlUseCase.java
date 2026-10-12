package bo.phishshield.backend.analisis.application;

import bo.phishshield.backend.analisis.domain.model.Alerta;
import bo.phishshield.backend.analisis.domain.model.Analisis;
import bo.phishshield.backend.analisis.domain.model.DominioListado;
import bo.phishshield.backend.analisis.domain.model.DominioOficial;
import bo.phishshield.backend.analisis.domain.model.Indicador;
import bo.phishshield.backend.analisis.domain.model.Sha256;
import bo.phishshield.backend.analisis.domain.model.TipoAnalisis;
import bo.phishshield.backend.analisis.domain.model.UrlAnalizada;
import bo.phishshield.backend.analisis.domain.port.out.AnalisisRepositoryPort;
import bo.phishshield.backend.analisis.domain.port.out.DominioOficialRepositoryPort;
import bo.phishshield.backend.analisis.domain.port.out.ListaNegraRepositoryPort;
import bo.phishshield.backend.analisis.domain.port.out.ReglaDeteccionRepositoryPort;
import bo.phishshield.backend.analisis.domain.service.MotorReglasUrl;
import bo.phishshield.backend.analisis.domain.service.ReglaListaNegra;
import bo.phishshield.backend.analisis.domain.service.ReglaTyposquatting;
import bo.phishshield.backend.analisis.domain.service.ReglaUrl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Caso de uso: analizar una URL y dejar el resultado persistido.
 *
 * Orquesta el flujo completo, pero no contiene reglas de negocio:
 * esas viven en el dominio. Su trabajo es coordinar quien hace que
 * y en que orden.
 */
public class AnalizarUrlUseCase {

    /**
     * Cuanto tiempo se reutiliza un analisis previo de la misma URL.
     * Pasado ese plazo se recalcula: la lista negra y los pesos pueden
     * haber cambiado, y un sitio puede pasar de legitimo a malicioso.
     */
    public static final Duration VIGENCIA_CACHE = Duration.ofHours(24);

    private static final String CATEGORIA_URL = "url";

    private final List<ReglaUrl> reglasBase;
    private final AnalisisRepositoryPort analisisRepository;
    private final ReglaDeteccionRepositoryPort reglaRepository;
    private final DominioOficialRepositoryPort dominioRepository;
    private final ListaNegraRepositoryPort listaNegraRepository;
    private final Clock reloj;

    public AnalizarUrlUseCase(List<ReglaUrl> reglasBase,
                              AnalisisRepositoryPort analisisRepository,
                              ReglaDeteccionRepositoryPort reglaRepository,
                              DominioOficialRepositoryPort dominioRepository,
                              ListaNegraRepositoryPort listaNegraRepository,
                              Clock reloj) {
        this.reglasBase = List.copyOf(reglasBase);
        this.analisisRepository = analisisRepository;
        this.reglaRepository = reglaRepository;
        this.dominioRepository = dominioRepository;
        this.listaNegraRepository = listaNegraRepository;
        this.reloj = reloj;
    }

    public Analisis ejecutar(ComandoAnalizarUrl comando) {
        long inicio = System.nanoTime();

        // 1. Parsear y normalizar. Si la URL es invalida, revienta aca.
        UrlAnalizada url = UrlAnalizada.de(comando.url());

        // 2. Cache por hash: si ya analizamos esta URL hace poco, lo devolvemos
        String hash = Sha256.de(url.getOriginal());
        Optional<Analisis> previo = analisisRepository.buscarPorHash(hash);
        if (previo.isPresent() && sigueVigente(previo.get())) {
            return previo.get();
        }

        // 3. Armar el motor con los datos de referencia de la base
        List<DominioOficial> oficiales = dominioRepository.listarActivos();
        List<DominioListado> listaNegra = listaNegraRepository.listarActivos();
        MotorReglasUrl motor = new MotorReglasUrl(reglasConDinamicas(oficiales, listaNegra));

        // 4. Aplicar las reglas con los pesos vigentes
        Map<String, BigDecimal> pesos = reglaRepository.pesosActivosPorCategoria(CATEGORIA_URL);
        List<Indicador> indicadores = motor.evaluar(url, pesos);

        // 5. Construir el agregado: el veredicto sale solo del puntaje
        Analisis analisis = Analisis.deUrl(url, comando.origen(), indicadores);
        analisis.asignarContexto(comando.idUsuario(), comando.idEmpresa(), null);
        analisis.registrarTiempo((int) ((System.nanoTime() - inicio) / 1_000_000));

        // 6. Persistir analisis + indicadores, y la alerta si no es seguro.
        //    Solo pasa con analisis nuevos: lo que sale de la cache ya
        //    tiene su alerta (alertas.id_analisis es UNIQUE).
        return Alerta.para(analisis)
                .map(alerta -> analisisRepository.guardarConAlerta(analisis, alerta))
                .orElseGet(() -> analisisRepository.guardar(analisis));
    }

    /**
     * Solo se reutiliza un analisis de URL con fecha conocida y dentro de la
     * vigencia. Asi las filas viejas (por ejemplo, las del seed) no se
     * devuelven para siempre.
     */
    private boolean sigueVigente(Analisis previo) {
        if (previo.getTipo() != TipoAnalisis.URL || previo.getFechaAnalisis() == null) {
            return false;
        }
        OffsetDateTime limite = OffsetDateTime.now(reloj).minus(VIGENCIA_CACHE);
        return previo.getFechaAnalisis().isAfter(limite);
    }

    /**
     * Typosquatting y lista negra dependen de datos que cambian con el tiempo,
     * asi que se construyen en cada ejecucion; las demas reglas son sin
     * estado y se reutilizan.
     */
    private List<ReglaUrl> reglasConDinamicas(List<DominioOficial> oficiales,
                                              List<DominioListado> listaNegra) {
        List<ReglaUrl> todas = new ArrayList<>(reglasBase);
        if (!oficiales.isEmpty()) {
            todas.add(new ReglaTyposquatting(oficiales));
        }
        if (!listaNegra.isEmpty()) {
            todas.add(new ReglaListaNegra(listaNegra));
        }
        return todas;
    }
}
