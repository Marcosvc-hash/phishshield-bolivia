package bo.phishshield.backend.analisis.application;

import bo.phishshield.backend.analisis.domain.model.Analisis;
import bo.phishshield.backend.analisis.domain.model.DominioOficial;
import bo.phishshield.backend.analisis.domain.model.OrigenAnalisis;
import bo.phishshield.backend.analisis.domain.model.Resultado;
import bo.phishshield.backend.analisis.domain.model.UrlInvalidaException;
import bo.phishshield.backend.analisis.domain.port.out.AnalisisRepositoryPort;
import bo.phishshield.backend.analisis.domain.port.out.DominioOficialRepositoryPort;
import bo.phishshield.backend.analisis.domain.port.out.ReglaDeteccionRepositoryPort;
import bo.phishshield.backend.analisis.domain.service.ReglaIpLiteral;
import bo.phishshield.backend.analisis.domain.service.ReglaSinHttps;
import bo.phishshield.backend.analisis.domain.service.ReglaSubdominios;
import bo.phishshield.backend.analisis.domain.service.ReglaTldSospechoso;
import bo.phishshield.backend.analisis.domain.service.ReglaUrl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AnalizarUrlUseCaseTest {

    // ---- Dobles de prueba: implementaciones en memoria de los puertos ----

    static class RepositorioEnMemoria implements AnalisisRepositoryPort {
        final List<Analisis> guardados = new ArrayList<>();

        @Override
        public Analisis guardar(Analisis analisis) {
            analisis.asignarIdentidad((long) (guardados.size() + 1),
                    UUID.randomUUID(), OffsetDateTime.now());
            guardados.add(analisis);
            return analisis;
        }

        @Override
        public Optional<Analisis> buscarPorHash(String contenidoHash) {
            return guardados.stream()
                    .filter(a -> a.getContenidoHash().equals(contenidoHash))
                    .findFirst();
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
            pesos.put("URL_IP_LITERAL", new BigDecimal("18.00"));
            pesos.put("URL_SIN_HTTPS", new BigDecimal("8.00"));
            pesos.put("URL_TYPOSQUATTING", new BigDecimal("25.00"));
            pesos.put("URL_TLD_SOSPECHOSO", new BigDecimal("12.00"));
            pesos.put("URL_SUBDOMINIOS", new BigDecimal("10.00"));
            return pesos;
        }
    }

    static class DominiosFijos implements DominioOficialRepositoryPort {
        @Override
        public List<DominioOficial> listarActivos() {
            return List.of(
                    new DominioOficial("Banco Nacional de Bolivia", "bnb.com.bo"),
                    new DominioOficial("Banco Union", "bancounion.com.bo"),
                    new DominioOficial("Impuestos Nacionales", "impuestos.gob.bo")
            );
        }
    }

    // ---- Montaje ----

    private final RepositorioEnMemoria repositorio = new RepositorioEnMemoria();

    private AnalizarUrlUseCase casoDeUso() {
        List<ReglaUrl> base = List.of(
                new ReglaIpLiteral(),
                new ReglaSinHttps(),
                new ReglaTldSospechoso(),
                new ReglaSubdominios()
        );
        return new AnalizarUrlUseCase(base, repositorio, new PesosFijos(), new DominiosFijos());
    }

    private Analisis analizar(String url) {
        return casoDeUso().ejecutar(
                new ComandoAnalizarUrl(url, OrigenAnalisis.WEB, 1L, 1L));
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
        Analisis resultado = analizar("http://bnb-bo-seguro.com/login/verificar-cuenta");

        String detalle = resultado.getIndicadores().stream()
                .map(i -> i.codigoRegla() + "=" + i.aporte())
                .toList()
                .toString();

        assertTrue(resultado.getIndicadores().stream()
                        .anyMatch(i -> i.codigoRegla().equals("URL_TYPOSQUATTING")),
                "Indicadores: " + detalle);

        assertTrue(resultado.getNivelRiesgo().compareTo(new BigDecimal("30")) > 0,
                "Riesgo=" + resultado.getNivelRiesgo() + " Indicadores: " + detalle);
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
}
