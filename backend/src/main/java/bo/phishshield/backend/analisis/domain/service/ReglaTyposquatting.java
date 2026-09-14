package bo.phishshield.backend.analisis.domain.service;

import bo.phishshield.backend.analisis.domain.model.DominioOficial;
import bo.phishshield.backend.analisis.domain.model.UrlAnalizada;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * URL_TYPOSQUATTING: detecta dominios que imitan a entidades bolivianas conocidas,
 * ya sea por similitud ortografica del dominio completo o porque la marca
 * aparece como palabra dentro de un dominio ajeno.
 */
public class ReglaTyposquatting implements ReglaUrl {

    private static final int DISTANCIA_MAXIMA = 2;
    private static final int LONGITUD_MINIMA_MARCA = 3;
    private static final Pattern SEPARADORES = Pattern.compile("[.\\-_]");

    private final List<DominioOficial> dominiosOficiales;

    public ReglaTyposquatting(List<DominioOficial> dominiosOficiales) {
        this.dominiosOficiales = List.copyOf(dominiosOficiales);
    }

    @Override
    public String codigo() {
        return "URL_TYPOSQUATTING";
    }

    @Override
    public Optional<String> evaluar(UrlAnalizada url) {
        String candidato = url.getDominioRegistrable();

        // Primera pasada: si el dominio es oficial, no hay nada que reportar
        for (DominioOficial oficial : dominiosOficiales) {
            if (candidato.equals(oficial.dominio())) {
                return Optional.empty();
            }
        }

        List<String> etiquetas = etiquetasDe(candidato);

        for (DominioOficial oficial : dominiosOficiales) {

            // Caso 1: escritura casi identica del dominio completo
            // (bancounlon.com.bo vs bancounion.com.bo)
            int distancia = distanciaLevenshtein(candidato, oficial.dominio());
            if (distancia > 0 && distancia <= DISTANCIA_MAXIMA) {
                return Optional.of(candidato + " se parece a " + oficial.dominio()
                        + " (distancia " + distancia + ")");
            }

            String marca = marcaDe(oficial.dominio());
            if (marca.length() < LONGITUD_MINIMA_MARCA) {
                continue;
            }

            for (String etiqueta : etiquetas) {

                // Caso 2: la marca aparece como palabra completa rodeada de ruido
                // (bnb-bo-seguro.com contiene la etiqueta exacta "bnb")
                if (etiqueta.equals(marca)) {
                    return Optional.of("El dominio usa la marca '" + marca
                            + "' de " + oficial.entidad() + " sin ser su dominio oficial");
                }

                // Caso 3: la marca aparece mal escrita como palabra completa.
                // La tolerancia se escala segun el largo y se exige que las
                // dos palabras tengan un tamano parecido, para que "urbnb"
                // no se confunda con "bnb".
                if (etiqueta.length() >= LONGITUD_MINIMA_MARCA) {
                    int d = distanciaLevenshtein(etiqueta, marca);
                    if (d > 0 && d <= toleranciaPara(marca)
                            && Math.abs(etiqueta.length() - marca.length()) <= 1) {
                        return Optional.of("El dominio contiene '" + etiqueta
                                + "', muy similar a la marca '" + marca
                                + "' de " + oficial.entidad());
                    }
                }
            }
        }

        return Optional.empty();
    }

    /** "bnb.com.bo" devuelve "bnb"; "impuestos.gob.bo" devuelve "impuestos" */
    private String marcaDe(String dominioOficial) {
        int primerPunto = dominioOficial.indexOf('.');
        return primerPunto < 0 ? dominioOficial : dominioOficial.substring(0, primerPunto);
    }

    /** "bnb-bo-seguro.com" devuelve ["bnb", "bo", "seguro", "com"] */
    private List<String> etiquetasDe(String dominio) {
        return Arrays.stream(SEPARADORES.split(dominio))
                .filter(s -> !s.isBlank())
                .toList();
    }

    /**
     * Cuanta diferencia ortografica se tolera antes de considerar que dos
     * palabras son la misma. Una marca corta no admite casi ninguna:
     * cambiar una letra de "bnb" produce otra palabra, no un error de tipeo.
     */
    private int toleranciaPara(String marca) {
        if (marca.length() <= 4) {
            return 0;
        }
        if (marca.length() <= 7) {
            return 1;
        }
        return DISTANCIA_MAXIMA;
    }

    /**
     * Distancia de edicion: cuantas inserciones, borrados o sustituciones
     * hacen falta para convertir una cadena en la otra.
     */
    private int distanciaLevenshtein(String a, String b) {
        int[] anterior = new int[b.length() + 1];
        int[] actual = new int[b.length() + 1];

        for (int j = 0; j <= b.length(); j++) {
            anterior[j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            actual[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int costo = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                actual[j] = Math.min(
                        Math.min(actual[j - 1] + 1, anterior[j] + 1),
                        anterior[j - 1] + costo);
            }
            int[] intercambio = anterior;
            anterior = actual;
            actual = intercambio;
        }

        return anterior[b.length()];
    }
}