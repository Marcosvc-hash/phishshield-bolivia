package bo.phishshield.backend.qa;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El dominio de la vertical qa no puede depender de frameworks: ni Spring,
 * ni JPA, ni las herramientas de prueba que usa la infraestructura (JUnit,
 * Playwright, Rest Assured). Es la misma regla que el docente revisa con
 * git grep, comprobada en cada mvnw test.
 */
class DominioQaLimpioTest {

    private static final Path DOMINIO = Path.of("src/main/java/bo/phishshield/backend/qa/domain");

    private static final Pattern IMPORT_PROHIBIDO = Pattern.compile(
            "^\\s*import\\s+(static\\s+)?(org\\.springframework|jakarta\\.persistence|org\\.junit"
                    + "|org\\.opentest4j|com\\.microsoft\\.playwright|io\\.restassured)\\b.*");

    @Test
    void ningun_archivo_del_dominio_qa_importa_frameworks() throws IOException {
        assertTrue(Files.isDirectory(DOMINIO), "No se encontró " + DOMINIO.toAbsolutePath());

        List<String> violaciones = new ArrayList<>();
        List<Path> archivos;
        try (Stream<Path> recorrido = Files.walk(DOMINIO)) {
            archivos = recorrido.filter(p -> p.toString().endsWith(".java")).toList();
        }
        for (Path archivo : archivos) {
            List<String> lineas = Files.readAllLines(archivo);
            for (int i = 0; i < lineas.size(); i++) {
                if (IMPORT_PROHIBIDO.matcher(lineas.get(i)).matches()) {
                    violaciones.add(DOMINIO.relativize(archivo) + ":" + (i + 1) + "  " + lineas.get(i).strip());
                }
            }
        }

        assertTrue(archivos.size() > 20, "Se esperaban los archivos del dominio qa, se encontraron " + archivos.size());
        assertEquals(List.of(), violaciones);
    }
}
