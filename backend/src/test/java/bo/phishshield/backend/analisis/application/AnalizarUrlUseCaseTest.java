package bo.phishshield.backend.analisis.application;

import bo.phishshield.backend.analisis.domain.model.Alerta;
import bo.phishshield.backend.analisis.domain.model.Analisis;
import bo.phishshield.backend.analisis.domain.model.CanalAlerta;
import bo.phishshield.backend.analisis.domain.model.Severidad;
import bo.phishshield.backend.analisis.domain.model.DominioListado;
import bo.phishshield.backend.analisis.domain.model.DominioOficial;
import bo.phishshield.backend.analisis.domain.model.Indicador;
import bo.phishshield.backend.analisis.domain.model.OrigenAnalisis;
import bo.phishshield.backend.analisis.domain.model.PrediccionIa;
import bo.phishshield.backend.analisis.domain.model.Resultado;
import bo.phishshield.backend.analisis.domain.model.Sha256;
import bo.phishshield.backend.analisis.domain.model.TipoAnalisis;
import bo.phishshield.backend.analisis.domain.model.UrlInvalidaException;
import bo.phishshield.backend.analisis.domain.port.out.AnalisisRepositoryPort;
import bo.phishshield.backend.analisis.domain.port.out.DominioOficialRepositoryPort;
import bo.phishshield.backend.analisis.domain.port.out.ListaBlancaRepositoryPort;
import bo.phishshield.backend.analisis.domain.port.out.ListaNegraRepositoryPort;
import bo.phishshield.backend.analisis.domain.port.out.ModeloIaPort;
import bo.phishshield.backend.analisis.domain.port.out.ModeloIaRepositoryPort;
import bo.phishshield.backend.analisis.domain.port.out.ReglaDeteccionRepositoryPort;
import bo.phishshield.backend.analisis.domain.service.ReglaIpLiteral;
import bo.phishshield.backend.analisis.domain.service.ReglaRutaSensible;
import bo.phishshield.backend.analisis.domain.service.ReglaSinHttps;
import bo.phishshield.backend.analisis.domain.service.ReglaSubdominios;
import bo.phishshield.backend.analisis.domain.service.ReglaTldSospechoso;
import bo.phishshield.backend.analisis.domain.service.ReglaUrl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AnalizarUrlUseCaseTest {

    /** Reloj fijo: las pruebas de vigencia no dependen de la hora real. */
    private static final Clock RELOJ = Clock.fixed(
            Instant.parse("2026-10-11T16:00:00Z"), ZoneOffset.ofHours(-4));

    private static final String URL_BNB_FALSO = "http://bnb-bo-seguro.com/login/verificar-cuenta";

    // ---- Dobles de prueba: implementaciones en memoria de los puertos ----

    static class RepositorioEnMemoria implements AnalisisRepositoryPort {
        final List<Analisis> guardados = new ArrayList<>();
        /** Alertas por id de analisis, como alertas.id_analisis UNIQUE. */
        final Map<Long, Alerta> alertas = new HashMap<>();

        @Override
        public Analisis guardar(Analisis analisis) {
            analisis.asignarIdentidad((long) (guardados.size() + 1),
                    UUID.randomUUID(), OffsetDateTime.now(RELOJ));
            guardados.add(analisis);
            return analisis;
        }

        @Override
        public Analisis guardarConAlerta(Analisis analisis, Alerta alerta) {
            Analisis guardado = guardar(analisis);
            if (alertas.putIfAbsent(guardado.getId(), alerta) != null) {
                throw new IllegalStateException("Alerta duplicada para el analisis " + guardado.getId());
            }
            return guardado;
        }

        /** Igual que el adaptador real: el mas reciente con ese hash. */
        @Override
        public Optional<Analisis> buscarPorHash(String contenidoHash) {
            return guardados.stream()
                    .filter(a -> a.getContenidoHash().equals(contenidoHash))
                    .max(Comparator.comparing(Analisis::getFechaAnalisis));
        }

        @Override
        public Optional<Analisis> buscarPorUuid(UUID uuidPublico) {
            return guardados.stream()
                    .filter(a -> uuidPublico.equals(a.getUuidPublico()))
                    .findFirst();
        }

        @Override
        public List<Analisis> listarUltimos(int limite) {
            return List.copyOf(guardados);
        }
    }

    static class PesosFijos implements ReglaDeteccionRepositoryPort {
        @Override
        public Map<String, BigDecimal> pesosActivosPorCategoria(String categoria) {
            Map<String, BigDecimal> pesos = new HashMap<>();
            pesos.put("URL_IP_LITERAL", new BigDecimal("30.00"));
            pesos.put("URL_SIN_HTTPS", new BigDecimal("8.00"));
            pesos.put("URL_TYPOSQUATTING", new BigDecimal("45.00"));
            pesos.put("URL_TLD_SOSPECHOSO", new BigDecimal("12.00"));
            pesos.put("URL_SUBDOMINIOS", new BigDecimal("10.00"));
            pesos.put("URL_LISTA_NEGRA", new BigDecimal("70.00"));
            pesos.put("URL_RUTA_SENSIBLE", new BigDecimal("10.00"));
            pesos.put("IA_MODELO_URL", new BigDecimal("40.00"));
            return pesos;
        }

        @Override
        public Set<String> codigosRegistrados() {
            return pesosActivosPorCategoria("url").keySet();
        }
    }

    static class DominiosFijos implements DominioOficialRepositoryPort {
        @Override
        public List<DominioOficial> listarActivos() {
            return List.of(
                    new DominioOficial("Banco Nacional de Bolivia", "bnb.com.bo"),
                    new DominioOficial("Banco Union", "bancounion.com.bo"),
                    new DominioOficial("Impuestos Nacionales", "impuestos.gob.bo"),
                    new DominioOficial("Banco BISA", "bisa.com")
            );
        }
    }

    /** Arranca con visa.com y bnb.com.bo, como el seed; cada prueba puede vaciarla. */
    static class ListaBlancaEnMemoria implements ListaBlancaRepositoryPort {
        final List<DominioListado> dominios = new ArrayList<>(List.of(
                new DominioListado("visa.com", "Se parece a bisa.com, pero es legitimo"),
                new DominioListado("bnb.com.bo", "Dominio oficial verificado")
        ));

        @Override
        public List<DominioListado> listarActivos() {
            return List.copyOf(dominios);
        }
    }

    /** Arranca con la lista negra del seed; cada prueba puede vaciarla. */
    static class ListaNegraEnMemoria implements ListaNegraRepositoryPort {
        final List<DominioListado> dominios = new ArrayList<>(List.of(
                new DominioListado("bnb-bo-seguro.com", "Clon de la banca en linea del BNB"),
                new DominioListado("bancounion-verificacion.net", "Formulario falso de verificacion"),
                new DominioListado("impuestos-gob-bo.info", "Suplanta al SIN pidiendo datos de NIT")
        ));

        @Override
        public List<DominioListado> listarActivos() {
            return List.copyOf(dominios);
        }
    }

    /**
     * Doble del modelo de IA. Por defecto no responde, como si el servicio
     * estuviera apagado: asi las pruebas de reglas no dependen de la IA.
     */
    static class ModeloIaFalso implements ModeloIaPort {
        Optional<PrediccionIa> respuesta = Optional.empty();
        final List<String> consultadas = new ArrayList<>();

        @Override
        public Optional<PrediccionIa> predecir(String url) {
            consultadas.add(url);
            return respuesta;
        }

        void responder(String probabilidad, String... senales) {
            respuesta = Optional.of(new PrediccionIa(new BigDecimal(probabilidad), UMBRAL_IA,
                    List.of(senales), "phishshield-url-ia", "1.0.0"));
        }
    }

    static final BigDecimal UMBRAL_IA = new BigDecimal("0.5527");
    static final long ID_MODELO_REAL = 4L;

    /** modelos_ia con el modelo real registrado. */
    static class ModelosRegistrados implements ModeloIaRepositoryPort {
        @Override
        public Optional<Long> buscarId(String nombre, String version) {
            return "phishshield-url-ia".equals(nombre) && "1.0.0".equals(version)
                    ? Optional.of(ID_MODELO_REAL) : Optional.empty();
        }
    }

    // ---- Montaje ----

    private final RepositorioEnMemoria repositorio = new RepositorioEnMemoria();
    private final ListaNegraEnMemoria listaNegra = new ListaNegraEnMemoria();
    private final ListaBlancaEnMemoria listaBlanca = new ListaBlancaEnMemoria();
    private final ModeloIaFalso modeloIa = new ModeloIaFalso();

    private AnalizarUrlUseCase casoDeUso() {
        List<ReglaUrl> base = List.of(
                new ReglaIpLiteral(),
                new ReglaSinHttps(),
                new ReglaTldSospechoso(),
                new ReglaSubdominios(),
                new ReglaRutaSensible()
        );
        return new AnalizarUrlUseCase(base, repositorio, new PesosFijos(),
                new DominiosFijos(), listaNegra, listaBlanca,
                modeloIa, new ModelosRegistrados(), RELOJ);
    }

    private Analisis analizar(String url) {
        return casoDeUso().ejecutar(
                new ComandoAnalizarUrl(url, OrigenAnalisis.WEB, 1L, 1L));
    }

    private static String detalle(Analisis analisis) {
        return analisis.getIndicadores().stream()
                .map(i -> i.codigoRegla() + "=" + i.aporte())
                .toList()
                .toString();
    }

    /** Simula la fila escrita a mano en 02_seed.sql, con la fecha indicada. */
    private Analisis filaDelSeed(OffsetDateTime fecha) {
        return Analisis.desdePersistencia(
                1L, UUID.randomUUID(), 3L, 1L, 2L,
                TipoAnalisis.URL, OrigenAnalisis.EXTENSION,
                URL_BNB_FALSO, Sha256.de(URL_BNB_FALSO), "bnb-bo-seguro.com",
                new BigDecimal("94.50"), 180, fecha,
                List.of(
                        new Indicador("URL_TYPOSQUATTING", "bnb-bo-seguro.com ≈ bnb.com.bo", new BigDecimal("25.00")),
                        new Indicador("URL_SIN_HTTPS", "esquema http", new BigDecimal("8.00")),
                        new Indicador("URL_DOMINIO_NUEVO", "registrado hace 6 dias", new BigDecimal("15.00"))
                ));
    }

    // ---- Pruebas ----

    @Test
    void sitio_legitimo_queda_como_seguro() {
        Analisis resultado = analizar("https://www.bnb.com.bo/");

        assertEquals(Resultado.SEGURO, resultado.getResultado());
        assertEquals(0, resultado.getIndicadores().size());
        assertFalse(resultado.requiereAlerta());
    }

    @Test
    void dominio_que_imita_al_bnb_suma_typosquatting() {
        Analisis resultado = analizar(URL_BNB_FALSO);

        assertTrue(resultado.getIndicadores().stream()
                        .anyMatch(i -> i.codigoRegla().equals("URL_TYPOSQUATTING")),
                "Indicadores: " + detalle(resultado));

        assertTrue(resultado.getNivelRiesgo().compareTo(new BigDecimal("30")) > 0,
                "Riesgo=" + resultado.getNivelRiesgo() + " Indicadores: " + detalle(resultado));
    }

    @Test
    void dominio_en_lista_negra_queda_como_phishing() {
        Analisis resultado = analizar(URL_BNB_FALSO);

        assertEquals(Resultado.PHISHING, resultado.getResultado(),
                "Riesgo=" + resultado.getNivelRiesgo() + " Indicadores: " + detalle(resultado));
        assertTrue(resultado.getIndicadores().stream()
                        .anyMatch(i -> i.codigoRegla().equals("URL_LISTA_NEGRA")),
                "Indicadores: " + detalle(resultado));
        assertTrue(resultado.requiereAlerta());
    }

    /**
     * Documenta una limitacion conocida: sin la lista negra, imitar a un banco
     * solo suma typosquatting (45 puntos desde la migracion V3; antes 25, que
     * daba seguro). Queda en sospechoso, nunca en phishing.
     * Si se recalibran los pesos, esta prueba debe cambiar a proposito.
     */
    @Test
    void sin_lista_negra_un_typosquatting_aislado_no_llega_a_phishing() {
        listaNegra.dominios.clear();

        // Sin "/login": se mide el typosquatting aislado, sin URL_RUTA_SENSIBLE
        Analisis resultado = analizar("https://bancounion-verificacion.net/");

        assertTrue(resultado.getIndicadores().stream()
                        .anyMatch(i -> i.codigoRegla().equals("URL_TYPOSQUATTING")),
                "Indicadores: " + detalle(resultado));
        assertEquals(new BigDecimal("45.00"), resultado.getNivelRiesgo());
        assertEquals(Resultado.SOSPECHOSO, resultado.getResultado(),
                "Riesgo=" + resultado.getNivelRiesgo() + " Indicadores: " + detalle(resultado));
    }

    @Test
    void un_analisis_vencido_se_vuelve_a_calcular() {
        Analisis delSeed = filaDelSeed(OffsetDateTime.now(RELOJ).minusDays(30));
        repositorio.guardados.add(delSeed);

        Analisis resultado = analizar(URL_BNB_FALSO);

        assertEquals(2, repositorio.guardados.size(), "Debio crear una fila nueva");
        assertNotEquals(delSeed.getId(), resultado.getId());
        assertEquals(Resultado.PHISHING, resultado.getResultado());
        assertFalse(resultado.getIndicadores().stream()
                        .anyMatch(i -> i.codigoRegla().equals("URL_DOMINIO_NUEVO")),
                "URL_DOMINIO_NUEVO no esta implementada; venia del seed: " + detalle(resultado));
    }

    @Test
    void un_analisis_reciente_se_reutiliza() {
        Analisis reciente = filaDelSeed(OffsetDateTime.now(RELOJ).minusHours(2));
        repositorio.guardados.add(reciente);

        Analisis resultado = analizar(URL_BNB_FALSO);

        assertEquals(1, repositorio.guardados.size());
        assertSame(reciente, resultado);
    }

    // ---- Ruta sensible ----

    @Test
    void una_ip_con_ruta_de_banca_queda_como_sospechosa() {
        Analisis resultado = analizar("http://192.168.1.50/banca/login");

        assertEquals(new BigDecimal("48.00"), resultado.getNivelRiesgo(), "Indicadores: " + detalle(resultado));
        assertEquals(Resultado.SOSPECHOSO, resultado.getResultado());
    }

    @Test
    void el_panel_de_un_router_sigue_siendo_seguro() {
        Analisis resultado = analizar("http://192.168.1.1");

        assertEquals(new BigDecimal("38.00"), resultado.getNivelRiesgo(), "Indicadores: " + detalle(resultado));
        assertEquals(Resultado.SEGURO, resultado.getResultado());
    }

    @Test
    void el_login_del_sitio_oficial_sigue_siendo_seguro() {
        Analisis resultado = analizar("https://www.bnb.com.bo/login");

        assertEquals(Resultado.SEGURO, resultado.getResultado(), "Indicadores: " + detalle(resultado));
        assertEquals(new BigDecimal("10.00"), resultado.getNivelRiesgo());
    }

    // ---- Lista blanca ----

    @Test
    void un_dominio_en_lista_blanca_queda_como_seguro() {
        Analisis resultado = analizar("https://www.visa.com");

        assertEquals(Resultado.SEGURO, resultado.getResultado(), "Indicadores: " + detalle(resultado));
        assertTrue(resultado.getIndicadores().isEmpty(), "Indicadores: " + detalle(resultado));
    }

    /** Sin la lista blanca, visa.com se confunde con bisa.com: es el falso positivo original. */
    @Test
    void sin_lista_blanca_visa_se_confunde_con_bisa() {
        listaBlanca.dominios.clear();

        Analisis resultado = analizar("https://www.visa.com");

        assertTrue(resultado.getIndicadores().stream()
                        .anyMatch(i -> i.codigoRegla().equals("URL_TYPOSQUATTING")),
                "Indicadores: " + detalle(resultado));
    }

    // ---- Alertas ----

    @Test
    void un_phishing_crea_una_alerta_critica() {
        Analisis resultado = analizar(URL_BNB_FALSO);

        Alerta alerta = repositorio.alertas.get(resultado.getId());
        assertNotNull(alerta, "Riesgo=" + resultado.getNivelRiesgo());
        assertEquals(Severidad.CRITICA, alerta.severidad());
        assertEquals(CanalAlerta.EXTENSION, alerta.canal());
    }

    /** Typosquatting + sin HTTPS + TLD .xyz (45 + 8 + 12 = 65), fuera de la lista negra: sospechoso. */
    @Test
    void un_sospechoso_crea_una_alerta_media() {
        Analisis resultado = analizar("http://bnb-bo-seguro.xyz/promo");

        assertEquals(Resultado.SOSPECHOSO, resultado.getResultado(),
                "Riesgo=" + resultado.getNivelRiesgo() + " Indicadores: " + detalle(resultado));
        Alerta alerta = repositorio.alertas.get(resultado.getId());
        assertNotNull(alerta);
        assertEquals(Severidad.MEDIA, alerta.severidad());
    }

    @Test
    void un_sitio_seguro_no_crea_alerta() {
        analizar("https://www.bnb.com.bo/");

        assertEquals(1, repositorio.guardados.size());
        assertTrue(repositorio.alertas.isEmpty());
    }

    @Test
    void un_analisis_de_la_cache_no_duplica_la_alerta() {
        Analisis primero = analizar(URL_BNB_FALSO);
        Analisis segundo = analizar(URL_BNB_FALSO);

        assertSame(primero, segundo);
        assertEquals(1, repositorio.alertas.size());
    }

    @Test
    void lo_que_llega_por_el_plugin_de_correo_se_avisa_por_email() {
        Analisis resultado = casoDeUso().ejecutar(new ComandoAnalizarUrl(
                URL_BNB_FALSO, OrigenAnalisis.PLUGIN_CORREO, 1L, 1L));

        assertEquals(CanalAlerta.EMAIL, repositorio.alertas.get(resultado.getId()).canal());
    }

    @Test
    void el_analisis_queda_persistido() {
        analizar("https://premios-tigo-bo.xyz/sorteo");

        assertEquals(1, repositorio.guardados.size());
        assertNotNull(repositorio.guardados.get(0).getId());
        assertNotNull(repositorio.guardados.get(0).getUuidPublico());
    }

    @Test
    void la_segunda_vez_devuelve_el_analisis_cacheado() {
        Analisis primero = analizar("http://bnb-bo-seguro.com/login");
        Analisis segundo = analizar("http://bnb-bo-seguro.com/login");

        assertEquals(1, repositorio.guardados.size());
        assertEquals(primero.getId(), segundo.getId());
    }

    @Test
    void guarda_el_contexto_de_usuario_y_empresa() {
        Analisis resultado = analizar("http://192.168.1.50/banca");

        assertEquals(1L, resultado.getIdUsuario());
        assertEquals(1L, resultado.getIdEmpresa());
    }

    @Test
    void registra_el_tiempo_de_analisis() {
        Analisis resultado = analizar("https://sitio-cualquiera.com");

        assertNotNull(resultado.getTiempoMs());
        assertTrue(resultado.getTiempoMs() >= 0);
    }

    @Test
    void url_invalida_lanza_excepcion_de_dominio() {
        assertThrows(UrlInvalidaException.class, () -> analizar("no-es-una-url"));
        assertThrows(UrlInvalidaException.class, () -> analizar("ftp://archivos.com"));
    }

    @Test
    void comando_sin_url_es_rechazado() {
        assertThrows(IllegalArgumentException.class,
                () -> new ComandoAnalizarUrl("", OrigenAnalisis.WEB, 1L, 1L));
    }

    // ---- Modelo de IA ----

    private static Optional<Indicador> indicadorIa(Analisis analisis) {
        return analisis.getIndicadores().stream()
                .filter(i -> i.codigoRegla().equals("IA_MODELO_URL"))
                .findFirst();
    }

    /** Sin reglas activadas: el puntaje es solo el de la IA, 0.90 x 40 = 36. */
    @Test
    void con_ia_sobre_el_umbral_suma_probabilidad_por_peso() {
        modeloIa.responder("0.90", "tiene 3 subdominios", "el dominio contiene 'login'", "el dominio es largo");

        Analisis resultado = analizar("https://pagos-cuenta-segura.com/");

        Indicador ia = indicadorIa(resultado).orElseThrow(() -> new AssertionError(detalle(resultado)));
        assertEquals(new BigDecimal("36.00"), ia.aporte());
        assertEquals(new BigDecimal("36.00"), resultado.getNivelRiesgo(), detalle(resultado));
        assertTrue(ia.valor().contains("90%"), ia.valor());
        assertTrue(ia.valor().contains("tiene 3 subdominios"), ia.valor());
        assertEquals(ID_MODELO_REAL, resultado.getIdModelo());
    }

    @Test
    void la_ia_se_suma_a_las_reglas() {
        modeloIa.responder("0.80", "el dominio tiene 2 guiones");

        // Typosquatting 45 (bnb-bo-seguro.com fuera de la lista negra) + IA 0.80 x 40 = 77
        listaNegra.dominios.clear();
        Analisis resultado = analizar("https://bnb-bo-seguro.com/");

        assertEquals(new BigDecimal("77.00"), resultado.getNivelRiesgo(), detalle(resultado));
        assertEquals(Resultado.PHISHING, resultado.getResultado());
    }

    /** Bajo el umbral la IA no aporta, pero participo: el analisis guarda el modelo. */
    @Test
    void con_ia_bajo_el_umbral_no_suma_puntos() {
        modeloIa.responder("0.40", "el dominio es largo");

        Analisis resultado = analizar("https://pagos-cuenta-segura.com/");

        assertTrue(indicadorIa(resultado).isEmpty(), detalle(resultado));
        assertEquals(0, resultado.getNivelRiesgo().compareTo(BigDecimal.ZERO));
        assertEquals(ID_MODELO_REAL, resultado.getIdModelo());
    }

    /** El adaptador real devuelve vacio cuando el servicio falla o tarda mas de 2 s. */
    @Test
    void con_ia_caida_el_analisis_sigue_solo_con_las_reglas() {
        // modeloIa no responde (Optional.empty). Typosquatting 45 + sin HTTPS 8 + ruta 10 = 63
        listaNegra.dominios.clear();
        Analisis resultado = analizar("http://bnb-bo-seguro.com/login");

        assertEquals(1, modeloIa.consultadas.size(), "Debio intentar consultar la IA");
        assertEquals(new BigDecimal("63.00"), resultado.getNivelRiesgo(), detalle(resultado));
        assertEquals(Resultado.SOSPECHOSO, resultado.getResultado());
        assertTrue(indicadorIa(resultado).isEmpty());
        assertNull(resultado.getIdModelo());
        assertEquals(1, repositorio.guardados.size());
    }

    @Test
    void sin_fila_en_reglas_deteccion_la_ia_aporta_cero() {
        modeloIa.responder("0.95");
        AnalizarUrlUseCase sinPesoIa = new AnalizarUrlUseCase(
                List.of(new ReglaSinHttps()), repositorio,
                new ReglaDeteccionRepositoryPort() {
                    @Override
                    public Map<String, BigDecimal> pesosActivosPorCategoria(String categoria) {
                        return Map.of();
                    }

                    @Override
                    public Set<String> codigosRegistrados() {
                        return Set.of();
                    }
                },
                new DominiosFijos(), listaNegra, listaBlanca, modeloIa, new ModelosRegistrados(), RELOJ);

        Analisis resultado = sinPesoIa.ejecutar(
                new ComandoAnalizarUrl("https://pagos-cuenta-segura.com/", OrigenAnalisis.WEB, 1L, 1L));

        assertEquals(new BigDecimal("0.00"), indicadorIa(resultado).orElseThrow().aporte());
    }

    /** En el dataset todas las IP eran phishing: la IA no se consulta y decide URL_IP_LITERAL. */
    @Test
    void una_ip_literal_no_consulta_la_ia() {
        modeloIa.responder("1.00", "usa una direccion IP en lugar de un dominio");

        Analisis resultado = analizar("http://192.168.1.1");

        assertTrue(modeloIa.consultadas.isEmpty(), "Consulto la IA para una IP literal");
        assertEquals(new BigDecimal("38.00"), resultado.getNivelRiesgo(), detalle(resultado));
        assertEquals(Resultado.SEGURO, resultado.getResultado());
        assertNull(resultado.getIdModelo());
    }

    @Test
    void un_dominio_de_la_lista_blanca_no_consulta_la_ia() {
        modeloIa.responder("0.99", "no deberia usarse");

        Analisis resultado = analizar("https://www.bnb.com.bo/PortalBNB/Principal/BancaPersonas");

        assertTrue(modeloIa.consultadas.isEmpty(), "Consulto la IA para un dominio de la lista blanca");
        assertTrue(indicadorIa(resultado).isEmpty());
        assertNull(resultado.getIdModelo());
        assertEquals(Resultado.SEGURO, resultado.getResultado());
    }

    /**
     * La lista blanca compara el dominio registrable exacto: que el host
     * empiece con "bnb.com.bo." no lo hace oficial.
     */
    @Test
    void un_dominio_oficial_usado_como_subdominio_si_consulta_la_ia() {
        modeloIa.responder("1.00", "tiene 5 subdominios (bnb.com.bo.verificar.cuenta)");

        Analisis resultado = analizar("http://bnb.com.bo.verificar.cuenta.segura-login.xyz/");

        assertEquals(List.of("http://bnb.com.bo.verificar.cuenta.segura-login.xyz/"), modeloIa.consultadas);
        assertEquals(new BigDecimal("40.00"), indicadorIa(resultado).orElseThrow().aporte());
        assertEquals(ID_MODELO_REAL, resultado.getIdModelo());
    }
}
