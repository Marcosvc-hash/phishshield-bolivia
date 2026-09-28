package bo.phishshield.backend.analisis.application;

import bo.phishshield.backend.analisis.domain.model.Analisis;
import bo.phishshield.backend.analisis.domain.model.DominioOficial;
import bo.phishshield.backend.analisis.domain.model.Indicador;
import bo.phishshield.backend.analisis.domain.model.Sha256;
import bo.phishshield.backend.analisis.domain.model.UrlAnalizada;
import bo.phishshield.backend.analisis.domain.port.out.AnalisisRepositoryPort;
import bo.phishshield.backend.analisis.domain.port.out.DominioOficialRepositoryPort;
import bo.phishshield.backend.analisis.domain.port.out.ReglaDeteccionRepositoryPort;
import bo.phishshield.backend.analisis.domain.service.MotorReglasUrl;
import bo.phishshield.backend.analisis.domain.service.ReglaTyposquatting;
import bo.phishshield.backend.analisis.domain.service.ReglaUrl;

import java.math.BigDecimal;
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

    private static final String CATEGORIA_URL = "url";

    private final List<ReglaUrl> reglasBase;
    private final AnalisisRepositoryPort analisisRepository;
    private final ReglaDeteccionRepositoryPort reglaRepository;
    private final DominioOficialRepositoryPort dominioRepository;

    public AnalizarUrlUseCase(List<ReglaUrl> reglasBase,
                              AnalisisRepositoryPort analisisRepository,
                              ReglaDeteccionRepositoryPort reglaRepository,
                              DominioOficialRepositoryPort dominioRepository) {
        this.reglasBase = List.copyOf(reglasBase);
        this.analisisRepository = analisisRepository;
        this.reglaRepository = reglaRepository;
        this.dominioRepository = dominioRepository;
    }

    public Analisis ejecutar(ComandoAnalizarUrl comando) {
        long inicio = System.nanoTime();

        // 1. Parsear y normalizar. Si la URL es invalida, revienta aca.
        UrlAnalizada url = UrlAnalizada.de(comando.url());

        // 2. Cache por hash: si ya analizamos este contenido, lo devolvemos
        String hash = Sha256.de(url.getOriginal());
        Optional<Analisis> previo = analisisRepository.buscarPorHash(hash);
        if (previo.isPresent()) {
            return previo.get();
        }

        // 3. Armar el motor con los dominios oficiales de la base
        List<DominioOficial> oficiales = dominioRepository.listarActivos();
        MotorReglasUrl motor = new MotorReglasUrl(reglasConTyposquatting(oficiales));

        // 4. Aplicar las reglas con los pesos vigentes
        Map<String, BigDecimal> pesos = reglaRepository.pesosActivosPorCategoria(CATEGORIA_URL);
        List<Indicador> indicadores = motor.evaluar(url, pesos);

        // 5. Construir el agregado: el veredicto sale solo del puntaje
        Analisis analisis = Analisis.deUrl(url, comando.origen(), indicadores);
        analisis.asignarContexto(comando.idUsuario(), comando.idEmpresa(), null);
        analisis.registrarTiempo((int) ((System.nanoTime() - inicio) / 1_000_000));

        // 6. Persistir analisis + indicadores
        return analisisRepository.guardar(analisis);
    }

    /**
     * La regla de typosquatting necesita la lista de dominios oficiales,
     * que cambia con el tiempo. Se construye en cada ejecucion; las demas
     * reglas son sin estado y se reutilizan.
     */
    private List<ReglaUrl> reglasConTyposquatting(List<DominioOficial> oficiales) {
        List<ReglaUrl> todas = new ArrayList<>(reglasBase);
        if (!oficiales.isEmpty()) {
            todas.add(new ReglaTyposquatting(oficiales));
        }
        return todas;
    }
}