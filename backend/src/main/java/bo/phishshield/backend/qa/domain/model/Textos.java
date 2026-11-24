package bo.phishshield.backend.qa.domain.model;

/** Ayudas para limpiar los textos que llegan de las pruebas antes de guardarlos. */
final class Textos {

    /** Mensajes de error y observaciones: suficiente para una traza corta, no para un volcado entero. */
    static final int MAXIMO_MENSAJE = 4000;

    private Textos() {
    }

    /**
     * Texto vacio o solo espacios se guarda como null. Se quita el caracter
     * nulo (codigo 0): PostgreSQL no lo admite en un texto y haria fallar el guardado.
     */
    static String opcional(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.indexOf('\u0000') < 0 ? texto : texto.replace("\u0000", "");
        return limpio.isBlank() ? null : limpio.strip();
    }

    /** Corta el texto con "…" al final si pasa del maximo. */
    static String recortar(String texto, int maximo) {
        if (texto == null || texto.length() <= maximo) {
            return texto;
        }
        return texto.substring(0, maximo - 1) + "…";
    }

    static String mensaje(String texto) {
        return recortar(opcional(texto), MAXIMO_MENSAJE);
    }
}
