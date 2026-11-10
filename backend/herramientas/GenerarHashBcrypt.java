import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.BufferedReader;
import java.io.Console;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Genera el hash BCrypt de una contrasena, para guardarlo en usuarios.password_hash.
 *
 * La contrasena se pide por teclado SIN mostrarla y nunca va como argumento,
 * asi no queda en el historial de PowerShell. Uso (desde backend/):
 *
 *   .\mvnw.cmd -q dependency:build-classpath "-Dmdep.outputFile=target\cp.txt"
 *   java -cp (Get-Content target\cp.txt) herramientas\GenerarHashBcrypt.java
 *
 * Usa el mismo costo (10) que BCryptPasswordHasherAdapter.
 */
public class GenerarHashBcrypt {

    public static void main(String[] args) throws Exception {
        String contrasena;
        // Desde Java 22, System.console() existe aunque la entrada venga de una
        // tuberia; isTerminal() dice si de verdad hay una persona escribiendo.
        Console consola = System.console();
        if (consola != null && consola.isTerminal()) {
            char[] escrita = consola.readPassword("Contrasena (no se muestra): ");
            char[] repetida = consola.readPassword("Repetila: ");
            if (!java.util.Arrays.equals(escrita, repetida)) {
                System.err.println("Las contrasenas no coinciden.");
                System.exit(1);
            }
            contrasena = new String(escrita);
        } else {
            // Sin consola interactiva (por ejemplo, entrada redirigida): una linea de stdin
            contrasena = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)).readLine();
            // PowerShell 5.1 puede anteponer un BOM (U+FEFF) al pasar texto por la tuberia
            if (contrasena != null && contrasena.startsWith("﻿")) {
                contrasena = contrasena.substring(1);
            }
        }

        if (contrasena == null || contrasena.length() < 8
                || contrasena.getBytes(StandardCharsets.UTF_8).length > 72) {
            System.err.println("La contrasena debe tener entre 8 caracteres y 72 bytes.");
            System.exit(1);
        }
        System.out.println(new BCryptPasswordEncoder(10).encode(contrasena));
    }
}
