package bo.phishshield.backend.qa.domain.model;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Correo al que se envian los reportes. Se guarda en minusculas y con un
 * formato mas estricto que chk_qa_ejec_correo (todo lo que acepta este
 * objeto, la base tambien lo acepta).
 */
public record CorreoDestino(String valor) {

    public static final int LONGITUD_MAXIMA = 150;

    /** usuario@dominio.ext: letras, digitos y . _ % + - antes de la @; extension de 2 letras o mas. */
    private static final Pattern FORMATO = Pattern.compile(
            "^[a-z0-9._%+-]+@[a-z0-9-]+(\\.[a-z0-9-]+)*\\.[a-z]{2,}$");

    public CorreoDestino {
        if (valor == null || valor.isBlank()) {
            throw new CorreoInvalidoException("Escriba el correo del destinatario");
        }
        valor = valor.strip().toLowerCase(Locale.ROOT);
        if (valor.length() > LONGITUD_MAXIMA) {
            throw new CorreoInvalidoException(
                    "El correo no puede tener más de " + LONGITUD_MAXIMA + " caracteres");
        }
        if (!FORMATO.matcher(valor).matches() || valor.contains("..")) {
            throw new CorreoInvalidoException(
                    "El correo no tiene un formato válido (ejemplo: nombre@dominio.com)");
        }
    }

    @Override
    public String toString() {
        return valor;
    }
}
