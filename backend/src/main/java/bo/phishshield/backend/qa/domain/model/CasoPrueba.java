package bo.phishshield.backend.qa.domain.model;

import java.util.regex.Pattern;

/**
 * Un caso del catalogo (tabla qa_casos). Los metadatos que salen en los
 * reportes (objetivo, precondiciones, datos, resultado esperado) viven aqui,
 * no en el codigo de la prueba: el metodo de prueba solo lleva el codigo
 * en su anotacion @CasoQA.
 */
public record CasoPrueba(
        Long id,
        String codigo,
        Suite suite,
        Herramienta herramienta,
        int orden,
        String nombre,
        String objetivo,
        String precondiciones,
        String datosPrueba,
        String resultadoEsperado,
        boolean activo
) {

    /** Igual que chk_qa_casos_codigo. */
    private static final Pattern FORMATO_CODIGO = Pattern.compile("^TC-[A-Z0-9]+-[0-9]{3}$");

    public CasoPrueba {
        if (codigo == null || !FORMATO_CODIGO.matcher(codigo).matches()) {
            throw new IllegalArgumentException("Código de caso inválido: " + codigo + " (formato TC-XXX-000)");
        }
        if (suite == null || herramienta == null) {
            throw new IllegalArgumentException("El caso " + codigo + " necesita suite y herramienta");
        }
        if (!codigo.startsWith("TC-" + suite.prefijo() + "-")) {
            throw new IllegalArgumentException("El caso " + codigo + " no corresponde a la suite "
                    + suite.etiqueta() + " (sus códigos empiezan con TC-" + suite.prefijo() + "-)");
        }
        nombre = obligatorio(nombre, "nombre", codigo);
        objetivo = obligatorio(objetivo, "objetivo", codigo);
        precondiciones = obligatorio(precondiciones, "precondiciones", codigo);
        datosPrueba = obligatorio(datosPrueba, "datos de prueba", codigo);
        resultadoEsperado = obligatorio(resultadoEsperado, "resultado esperado", codigo);
    }

    /** El tipo sale de la suite: un caso no puede contradecirlo. */
    public TipoPrueba tipo() {
        return suite.tipo();
    }

    private static String obligatorio(String texto, String campo, String codigo) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Al caso " + codigo + " le falta: " + campo);
        }
        return texto.strip();
    }
}
