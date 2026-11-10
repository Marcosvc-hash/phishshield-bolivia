package bo.phishshield.backend.listas.domain.model;

import bo.phishshield.backend.analisis.domain.model.UrlAnalizada;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Un nombre de dominio ya normalizado y validado: "Banco-Falso.COM. " se
 * guarda como "banco-falso.com". Es un value object: si existe, es valido
 * (no hace falta volver a revisarlo en el resto del codigo).
 *
 * Reglas:
 * - minusculas (chk_dominios_minus lo exige en la base), sin espacios ni
 *   punto final;
 * - solo el dominio: sin http://, ruta, puerto ni usuario;
 * - etiquetas de 1 a 63 caracteres (letras, digitos y guiones, sin guion
 *   al principio ni al final), 253 caracteres en total;
 * - al menos dos etiquetas y un TLD de letras (o punycode "xn--"): asi se
 *   rechazan las IP y los nombres sueltos;
 * - no puede ser un sufijo publico entero ("com.bo"): pondria en la lista
 *   a todos los dominios bolivianos.
 */
public record NombreDominio(String valor) {

    private static final int LARGO_MAXIMO = 253;
    private static final Pattern ETIQUETA = Pattern.compile("[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?");
    private static final Pattern TLD = Pattern.compile("[a-z]{2,63}|xn--[a-z0-9-]{1,59}");

    public NombreDominio {
        valor = normalizar(valor);
        validar(valor);
    }

    public static NombreDominio de(String texto) {
        return new NombreDominio(texto);
    }

    /**
     * El dominio que alguien registro: "login.banco.com.bo" -> "banco.com.bo".
     * Usa la misma regla que el motor (UrlAnalizada), para que las listas y
     * las reglas coincidan.
     */
    public String registrable() {
        return UrlAnalizada.de("https://" + valor).getDominioRegistrable();
    }

    public boolean esRegistrable() {
        return valor.equals(registrable());
    }

    private static String normalizar(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("El dominio es obligatorio");
        }
        String limpio = texto.strip().toLowerCase(Locale.ROOT);
        while (limpio.endsWith(".")) {
            limpio = limpio.substring(0, limpio.length() - 1);
        }
        return limpio;
    }

    private static void validar(String valor) {
        if (valor.contains("://") || valor.contains("/") || valor.contains("?") || valor.contains("#")
                || valor.contains("@") || valor.contains(":")) {
            throw new IllegalArgumentException(
                    "Escriba solo el dominio, sin http://, rutas ni puertos (ejemplo: banco-falso.com)");
        }
        if (!valor.chars().allMatch(c -> c < 128)) {
            throw new IllegalArgumentException(
                    "El dominio tiene caracteres no ASCII; escribalo en punycode (xn--...)");
        }
        if (valor.length() > LARGO_MAXIMO) {
            throw new IllegalArgumentException("El dominio no puede superar los " + LARGO_MAXIMO + " caracteres");
        }

        String[] etiquetas = valor.split("\\.", -1);
        if (etiquetas.length < 2) {
            throw new IllegalArgumentException("Falta la extension del dominio (ejemplo: .com o .com.bo): " + valor);
        }
        for (String etiqueta : etiquetas) {
            if (!ETIQUETA.matcher(etiqueta).matches()) {
                throw new IllegalArgumentException("Formato de dominio invalido: " + valor);
            }
        }
        if (!TLD.matcher(etiquetas[etiquetas.length - 1]).matches()) {
            throw new IllegalArgumentException(
                    "La extension debe tener letras (las direcciones IP no se aceptan): " + valor);
        }
        if (UrlAnalizada.de("https://" + valor).getSufijoPublico().equals(valor)) {
            throw new IllegalArgumentException(valor + " es una extension publica completa, no un dominio");
        }
    }

    @Override
    public String toString() {
        return valor;
    }
}
