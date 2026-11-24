package bo.phishshield.backend.qa.application;

import bo.phishshield.backend.qa.domain.model.AlcanceEjecucion;
import bo.phishshield.backend.qa.domain.model.CasoPrueba;
import bo.phishshield.backend.qa.domain.model.CorreoDestino;
import bo.phishshield.backend.qa.domain.model.Ejecucion;
import bo.phishshield.backend.qa.domain.model.EjecucionEnCursoException;
import bo.phishshield.backend.qa.domain.model.SeleccionInvalidaException;
import bo.phishshield.backend.qa.domain.model.Suite;
import bo.phishshield.backend.qa.domain.port.out.CatalogoCasosPort;
import bo.phishshield.backend.qa.domain.port.out.EjecucionRepositoryPort;
import bo.phishshield.backend.qa.domain.port.out.PlanificadorEjecucionPort;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Caso de uso: lanzar una ejecucion del Centro de Pruebas.
 *
 * Solo el ADMIN de la plataforma. Se ejecuta una a la vez (una prueba de
 * carga no debe mezclarse con otra ejecucion). Crea la ejecucion PENDIENTE
 * con sus casos en NOT_RUN y la deja programada: vuelve enseguida, sin
 * esperar a que las pruebas corran.
 */
public class IniciarEjecucionUseCase {

    public static final int MAXIMO_PARAMETROS = 20;
    public static final int MAXIMO_VALOR_PARAMETRO = 200;
    private static final Pattern NOMBRE_PARAMETRO = Pattern.compile("^[A-Za-z][A-Za-z0-9_.]{0,39}$");

    private final CatalogoCasosPort catalogo;
    private final EjecucionRepositoryPort ejecuciones;
    private final PlanificadorEjecucionPort planificador;
    private final String ambiente;
    private final Clock reloj;

    public IniciarEjecucionUseCase(CatalogoCasosPort catalogo, EjecucionRepositoryPort ejecuciones,
                                   PlanificadorEjecucionPort planificador, String ambiente, Clock reloj) {
        this.catalogo = catalogo;
        this.ejecuciones = ejecuciones;
        this.planificador = planificador;
        this.ambiente = ambiente;
        this.reloj = reloj;
    }

    public Ejecucion ejecutar(UsuarioActual quien, ComandoIniciarEjecucion comando) {
        AlcanceDatos.soloPlataforma(quien);

        ejecuciones.buscarActiva().ifPresent(activa -> {
            throw new EjecucionEnCursoException(activa);
        });

        List<CasoPrueba> casos;
        Suite suite;
        AlcanceEjecucion alcance;
        if (comando.porCodigos()) {
            casos = casosPorCodigo(comando.codigos());
            suite = comando.suite() != null ? comando.suite() : casos.getFirst().suite();
            alcance = AlcanceEjecucion.SELECCION;
        } else {
            if (comando.suite() == null) {
                throw new SeleccionInvalidaException("Elija una suite o marque al menos un caso");
            }
            suite = comando.suite();
            casos = catalogo.listarPorSuite(suite);
            if (casos.isEmpty()) {
                throw new SeleccionInvalidaException("La suite " + suite.etiqueta() + " no tiene casos activos");
            }
            alcance = AlcanceEjecucion.SUITE_COMPLETA;
        }

        // Sin destinatario explicito, los reportes van a quien lanzo la ejecucion
        CorreoDestino correo = new CorreoDestino(
                comando.correo() == null || comando.correo().isBlank() ? quien.email() : comando.correo());

        Ejecucion nueva = Ejecucion.nueva(quien.idUsuario(), suite, alcance, correo, ambiente,
                validarParametros(comando.parametros()), casos, OffsetDateTime.now(reloj));

        Ejecucion guardada = ejecuciones.guardar(nueva);
        planificador.programar(guardada.getUuidPublico());
        return guardada;
    }

    /** Los codigos se normalizan (TC-smk-001 = TC-SMK-001); uno repetido o inexistente es un error. */
    private List<CasoPrueba> casosPorCodigo(List<String> pedidos) {
        Set<String> codigos = new HashSet<>();
        List<String> ordenados = new ArrayList<>();
        for (String pedido : pedidos) {
            if (pedido == null || pedido.isBlank()) {
                throw new SeleccionInvalidaException("Hay un código de caso vacío");
            }
            String codigo = pedido.strip().toUpperCase(Locale.ROOT);
            if (!codigos.add(codigo)) {
                throw new SeleccionInvalidaException("El caso " + codigo + " está repetido");
            }
            ordenados.add(codigo);
        }

        List<CasoPrueba> encontrados = catalogo.buscarPorCodigos(codigos);
        Set<String> existentes = new HashSet<>();
        encontrados.forEach(c -> existentes.add(c.codigo()));
        List<String> faltantes = ordenados.stream().filter(c -> !existentes.contains(c)).toList();
        if (!faltantes.isEmpty()) {
            throw new SeleccionInvalidaException("No existen los casos: " + String.join(", ", faltantes));
        }
        return encontrados;
    }

    private static Map<String, String> validarParametros(Map<String, String> parametros) {
        if (parametros.size() > MAXIMO_PARAMETROS) {
            throw new SeleccionInvalidaException("Se admiten como máximo " + MAXIMO_PARAMETROS + " parámetros");
        }
        Map<String, String> limpios = new LinkedHashMap<>();
        parametros.forEach((nombre, valor) -> {
            if (nombre == null || !NOMBRE_PARAMETRO.matcher(nombre).matches()) {
                throw new SeleccionInvalidaException("Nombre de parámetro inválido: " + nombre
                        + " (letras, dígitos, punto o guion bajo; máximo 40)");
            }
            String texto = valor == null ? "" : valor.strip();
            if (texto.length() > MAXIMO_VALOR_PARAMETRO) {
                throw new SeleccionInvalidaException("El parámetro " + nombre + " es demasiado largo");
            }
            limpios.put(nombre, texto);
        });
        return limpios;
    }
}
