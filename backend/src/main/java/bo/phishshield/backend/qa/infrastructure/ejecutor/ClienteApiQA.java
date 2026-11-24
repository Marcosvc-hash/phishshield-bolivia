package bo.phishshield.backend.qa.infrastructure.ejecutor;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import io.restassured.specification.RequestSpecification;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Cliente HTTP de los casos de API, construido sobre Rest Assured.
 *
 * Cada peticion queda en la evidencia del paso que la hizo (metodo, URL,
 * cuerpo, estado, tiempo y respuesta), siempre ENMASCARADA: la contrasena,
 * los tokens y el encabezado Authorization nunca se guardan.
 */
public final class ClienteApiQA {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Campos JSON cuyo valor nunca se escribe en la evidencia. */
    private static final Pattern CAMPOS_SECRETOS = Pattern.compile(
            "(\"(?:contrasena|password|tokenAcceso|tokenRefresco|token)\"\\s*:\\s*\")[^\"]*(\")");
    private static final int MAXIMO_CUERPO = 1500;

    /** 5 s para conectar y 30 s para responder: un servicio caido falla rapido como INFRAESTRUCTURA. */
    private static final RestAssuredConfig CONFIG = RestAssuredConfig.config().httpClient(
            HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout", 5_000)
                    .setParam("http.socket.timeout", 30_000));

    private final SesionEjecucion sesion;
    private final BitacoraCaso bitacora;

    ClienteApiQA(SesionEjecucion sesion, BitacoraCaso bitacora) {
        this.sesion = sesion;
        this.bitacora = bitacora;
    }

    /** Peticion al backend sin el encabezado Authorization. */
    public RequestSpecification sinSesion() {
        return hacia(sesion.entorno().baseUrl());
    }

    /** Peticion al backend con el token del administrador de prueba (se renueva si va a vencer). */
    public RequestSpecification conSesion() {
        return sinSesion().header("Authorization", "Bearer " + tokenAdmin());
    }

    /** Peticion a otro servicio (por ejemplo, el de IA), tambien registrada en la evidencia. */
    public RequestSpecification hacia(String baseUrl) {
        return RestAssured.given()
                .config(CONFIG)
                .baseUri(baseUrl)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .filter(new Captura());
    }

    /**
     * Devuelve un token vigente. Si no hay uno guardado (o le queda menos de
     * un minuto), inicia sesion con QA_TEST_USER / QA_TEST_PASSWORD y anota
     * en el paso actual que se renovo.
     */
    public String tokenAdmin() {
        String vigente = sesion.tokenVigente();
        if (vigente != null) {
            return vigente;
        }
        Response respuesta = sinSesion()
                .body(json("email", sesion.entorno().usuario(), "contrasena", sesion.entorno().contrasena()))
                .post("/api/auth/login");
        if (respuesta.statusCode() != 200) {
            throw new FalloInfraestructuraException("No se pudo iniciar sesión con el administrador de prueba "
                    + "(QA_TEST_USER): HTTP " + respuesta.statusCode() + ". Revise el usuario y la contraseña.");
        }
        String token = respuesta.jsonPath().getString("tokenAcceso");
        String vence = respuesta.jsonPath().getString("accesoExpiraEn");
        Instant hasta = vence == null ? Instant.now().plusSeconds(600) : OffsetDateTime.parse(vence).toInstant();
        sesion.guardarToken(token, hasta);
        return token;
    }

    /** Hora local a la que vence el token guardado, para la evidencia. */
    public String horaVencimientoToken() {
        Instant vence = sesion.tokenVence();
        return vence == null ? "-" : HORA.format(vence.atZone(ZONA));
    }

    /** JSON a partir de pares nombre/valor: json("url", "https://...", "origen", "api"). */
    public static String json(Object... paresNombreValor) {
        if (paresNombreValor.length % 2 != 0) {
            throw new IllegalArgumentException("json() recibe pares nombre/valor");
        }
        Map<String, Object> mapa = new LinkedHashMap<>();
        for (int i = 0; i < paresNombreValor.length; i += 2) {
            mapa.put(String.valueOf(paresNombreValor[i]), paresNombreValor[i + 1]);
        }
        return JSON.writeValueAsString(mapa);
    }

    /** Quita contrasenas y tokens de un texto JSON. Publico para poder probarlo. */
    public static String enmascarar(String texto) {
        if (texto == null) {
            return null;
        }
        return CAMPOS_SECRETOS.matcher(texto).replaceAll("$1***$2");
    }

    private static String recortar(String texto) {
        if (texto == null || texto.length() <= MAXIMO_CUERPO) {
            return texto;
        }
        return texto.substring(0, MAXIMO_CUERPO) + "… (" + texto.length() + " caracteres en total)";
    }

    /** Filtro de Rest Assured: anota cada peticion y su respuesta en el paso actual. */
    private final class Captura implements Filter {

        @Override
        public Response filter(FilterableRequestSpecification peticion, FilterableResponseSpecification spec,
                               FilterContext contexto) {
            StringBuilder detalle = new StringBuilder();
            detalle.append(peticion.getMethod()).append(' ').append(peticion.getURI());
            if (peticion.getHeaders().hasHeaderWithName("Authorization")) {
                detalle.append("\nAuthorization: Bearer ***");
            }
            Object cuerpo = peticion.getBody();
            if (cuerpo != null) {
                detalle.append("\nCuerpo: ").append(recortar(enmascarar(String.valueOf(cuerpo))));
            }
            long inicio = System.nanoTime();
            try {
                Response respuesta = contexto.next(peticion, spec);
                long ms = (System.nanoTime() - inicio) / 1_000_000;
                detalle.append("\n-> HTTP ").append(respuesta.statusCode()).append(" (").append(ms).append(" ms)");
                String texto = respuesta.asString();
                if (texto != null && !texto.isBlank()) {
                    detalle.append("\n").append(recortar(enmascarar(texto)));
                }
                return respuesta;
            } catch (RuntimeException ex) {
                detalle.append("\n-> sin respuesta: ").append(BitacoraCaso.resumen(ex));
                throw ex;
            } finally {
                bitacora.anotarDetalleApi(detalle.toString());
            }
        }
    }
}
