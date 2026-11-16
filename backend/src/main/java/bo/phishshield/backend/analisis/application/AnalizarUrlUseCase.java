package bo.phishshield.backend.analisis.application;

import bo.phishshield.backend.analisis.domain.model.Alerta;
import bo.phishshield.backend.analisis.domain.model.Analisis;
import bo.phishshield.backend.analisis.domain.model.DominioListado;
import bo.phishshield.backend.analisis.domain.model.DominioOficial;
import bo.phishshield.backend.analisis.domain.model.Indicador;
import bo.phishshield.backend.analisis.domain.model.PrediccionIa;
import bo.phishshield.backend.analisis.domain.model.Sha256;
import bo.phishshield.backend.analisis.domain.model.TipoAnalisis;
import bo.phishshield.backend.analisis.domain.model.UrlAnalizada;
import bo.phishshield.backend.analisis.domain.port.out.AnalisisRepositoryPort;
import bo.phishshield.backend.analisis.domain.port.out.CambiosListasPort;
import bo.phishshield.backend.analisis.domain.port.out.DominioOficialRepositoryPort;
import bo.phishshield.backend.analisis.domain.port.out.ListaBlancaRepositoryPort;
import bo.phishshield.backend.analisis.domain.port.out.ListaNegraRepositoryPort;
import bo.phishshield.backend.analisis.domain.port.out.ModeloIaPort;
import bo.phishshield.backend.analisis.domain.port.out.ModeloIaRepositoryPort;
import bo.phishshield.backend.analisis.domain.port.out.ReglaDeteccionRepositoryPort;
import bo.phishshield.backend.analisis.domain.service.IndicadorIa;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final ListaBlancaRepositoryPort listaBlancaRepository;
    private final ModeloIaPort modeloIa;
    private final ModeloIaRepositoryPort modeloIaRepository;
    private final CambiosListasPort cambiosListas;
    private final Clock reloj;

    public AnalizarUrlUseCase(List<ReglaUrl> reglasBase,
                              AnalisisRepositoryPort analisisRepository,
                              ReglaDeteccionRepositoryPort reglaRepository,
                              DominioOficialRepositoryPort dominioRepository,
                              ListaNegraRepositoryPort listaNegraRepository,
                              ListaBlancaRepositoryPort listaBlancaRepository,
                              ModeloIaPort modeloIa,
                              ModeloIaRepositoryPort modeloIaRepository,
                              CambiosListasPort cambiosListas,
                              Clock reloj) {
        this.reglasBase = List.copyOf(reglasBase);
        this.analisisRepository = analisisRepository;
        this.reglaRepository = reglaRepository;
        this.dominioRepository = dominioRepository;
        this.listaNegraRepository = listaNegraRepository;
        this.listaBlancaRepository = listaBlancaRepository;
        this.modeloIa = modeloIa;
        this.modeloIaRepository = modeloIaRepository;
        this.cambiosListas = cambiosListas;
        this.reloj = reloj;
    }

    public Analisis ejecutar(ComandoAnalizarUrl comando) {
        long inicio = System.nanoTime();

        // 1. Parsear y normalizar. Si la URL es invalida, revienta aca.
        UrlAnalizada url = UrlAnalizada.de(comando.url());

        // 2. Cache por hash: si ya analizamos esta URL hace poco, no se recalcula
        String hash = Sha256.de(url.getOriginal());
        Optional<Analisis> previo = analisisRepository.buscarPorHash(hash)
                .filter(this::sigueVigente);
        if (previo.isPresent()) {
            return reutilizar(previo.get(), comando, inicio);
        }

        // 3. Armar el motor con los datos de referencia de la base
        List<DominioOficial> oficiales = dominioRepository.listarActivos();
        List<DominioListado> listaNegra = listaNegraRepository.listarActivos();
        Set<String> listaBlanca = dominiosDe(listaBlancaRepository.listarActivos());
        MotorReglasUrl motor = new MotorReglasUrl(
                reglasConDinamicas(oficiales, listaNegra, listaBlanca));

        // 4. Aplicar las reglas con los pesos vigentes
        Map<String, BigDecimal> pesos = reglaRepository.pesosActivosPorCategoria(CATEGORIA_URL);
        List<Indicador> indicadores = new ArrayList<>(motor.evaluar(url, pesos));

        // 4b. Modelo de IA: una senal mas, que se suma solo si supera su umbral.
        //     Si el servicio no responde, el puerto devuelve vacio y quedan las reglas.
        Optional<PrediccionIa> prediccion = consultarIa(url, listaBlanca);
        prediccion.flatMap(p -> IndicadorIa.desde(p, pesos.get(IndicadorIa.CODIGO)))
                .ifPresent(indicadores::add);
        Long idModelo = prediccion
                .flatMap(p -> modeloIaRepository.buscarId(p.modelo(), p.version()))
                .orElse(null);

        // 5. Construir el agregado: el veredicto sale solo del puntaje
        Analisis analisis = Analisis.deUrl(url, comando.origen(), indicadores);
        analisis.asignarContexto(comando.idUsuario(), comando.idEmpresa(), idModelo);
        analisis.registrarTiempo(milisegundosDesde(inicio));

        // 6. Persistir analisis + indicadores, y la alerta si no es seguro
        return persistir(analisis);
    }

    /**
     * Cache: los indicadores no se recalculan, pero cada usuario o empresa
     * deja su propia fila. Antes se devolvia la fila de quien analizo
     * primero, y el analisis no aparecia en el historial del segundo usuario
     * ni en el panel de su empresa, ni su empresa recibia la alerta.
     *
     * Si quien pide es el mismo de la fila previa (mismo usuario y misma
     * empresa; los anonimos cuentan como uno solo), se devuelve esa fila sin
     * escribir nada: repetir la consulta no duplica analisis ni alertas.
     */
    private Analisis reutilizar(Analisis previo, ComandoAnalizarUrl comando, long inicio) {
        boolean mismoSolicitante = Objects.equals(previo.getIdUsuario(), comando.idUsuario())
                && Objects.equals(previo.getIdEmpresa(), comando.idEmpresa());
        if (mismoSolicitante) {
            return previo;
        }
        Analisis copia = Analisis.reutilizando(previo, comando.origen());
        // El modelo de IA es parte del calculo que se copia
        copia.asignarContexto(comando.idUsuario(), comando.idEmpresa(), previo.getIdModelo());
        copia.registrarTiempo(milisegundosDesde(inicio));
        return persistir(copia);
    }

    /**
     * Guarda el analisis con sus indicadores y, si no es seguro, su alerta,
     * en una sola transaccion. Cada fila nueva (calculada o copiada de la
     * cache) tiene su propia alerta: alertas.id_analisis es UNIQUE, y la
     * alerta de otra empresa no la ve el admin de esta.
     */
    private Analisis persistir(Analisis analisis) {
        return Alerta.para(analisis)
                .map(alerta -> analisisRepository.guardarConAlerta(analisis, alerta))
                .orElseGet(() -> analisisRepository.guardar(analisis));
    }

    private static int milisegundosDesde(long inicioNanos) {
        return (int) ((System.nanoTime() - inicioNanos) / 1_000_000);
    }

    /**
     * Codigos de todas las reglas que este caso de uso puede aplicar,
     * incluidas las dinamicas. Sirve para verificar al arrancar que cada
     * una tenga su fila en reglas_deteccion.
     */
    public List<String> codigosDeReglas() {
        List<String> codigos = new ArrayList<>();
        for (ReglaUrl regla : reglasConDinamicas(
                List.of(new DominioOficial("referencia", "referencia.bo")),
                List.of(new DominioListado("referencia.bo", null)),
                Set.of())) {
            codigos.add(regla.codigo());
        }
        codigos.add(IndicadorIa.CODIGO);
        return List.copyOf(codigos);
    }

    /**
     * Los dominios de la lista blanca no se consultan: ya estan verificados, y
     * el modelo conoce pocos dominios .bo (ver ia/FICHA_MODELO.md). La
     * comparacion es exacta por dominio registrable, asi que
     * bnb.com.bo.verificar.segura-login.xyz (registrable segura-login.xyz)
     * si se consulta aunque bnb.com.bo este en la lista blanca.
     *
     * Las IP literales tampoco se consultan: en el dataset de entrenamiento
     * las 608 URL con IP son phishing, asi que el modelo solo repetiria
     * URL_IP_LITERAL (y marcaria como phishing el panel de un router).
     */
    private Optional<PrediccionIa> consultarIa(UrlAnalizada url, Set<String> listaBlanca) {
        if (url.hostEsIpLiteral() || listaBlanca.contains(url.getDominioRegistrable())) {
            return Optional.empty();
        }
        return modeloIa.predecir(url.getOriginal());
    }

    /**
     * Solo se reutiliza un analisis de URL con fecha conocida y dentro de la
     * vigencia. Asi las filas viejas (por ejemplo, las del seed) no se
     * devuelven para siempre.
     *
     * Ademas, si un administrador cambio una lista blanca o negra DESPUES de
     * ese analisis, ya no sirve: se calculo con las listas viejas. Sin esto,
     * un dominio recien agregado a la lista negra seguiria saliendo "seguro"
     * hasta 24 h para quien ya lo hubiera analizado.
     *
     * Las dos comparaciones usan el momento del CALCULO, no la fecha de la
     * fila: si la fila es una copia de la cache, cuenta cuando se calcularon
     * sus indicadores (ver Analisis.reutilizando).
     */
    private boolean sigueVigente(Analisis previo) {
        OffsetDateTime calculo = previo.momentoDelCalculo();
        if (previo.getTipo() != TipoAnalisis.URL || calculo == null) {
            return false;
        }
        OffsetDateTime limite = OffsetDateTime.now(reloj).minus(VIGENCIA_CACHE);
        if (!calculo.isAfter(limite)) {
            return false;
        }
        return cambiosListas.ultimoCambio()
                .map(calculo::isAfter)
                .orElse(true);
    }

    /**
     * Typosquatting y lista negra dependen de datos que cambian con el tiempo,
     * asi que se construyen en cada ejecucion; las demas reglas son sin
     * estado y se reutilizan.
     */
    private List<ReglaUrl> reglasConDinamicas(List<DominioOficial> oficiales,
                                              List<DominioListado> listaNegra,
                                              Set<String> listaBlanca) {
        List<ReglaUrl> todas = new ArrayList<>(reglasBase);
        if (!oficiales.isEmpty()) {
            todas.add(new ReglaTyposquatting(oficiales, listaBlanca));
        }
        if (!listaNegra.isEmpty()) {
            todas.add(new ReglaListaNegra(listaNegra));
        }
        return todas;
    }

    private static Set<String> dominiosDe(List<DominioListado> listados) {
        return listados.stream().map(DominioListado::dominio).collect(Collectors.toSet());
    }
}
