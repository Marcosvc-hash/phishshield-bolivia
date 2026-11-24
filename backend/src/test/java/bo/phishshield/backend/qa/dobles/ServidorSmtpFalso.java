package bo.phishshield.backend.qa.dobles;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Servidor SMTP de mentira para las pruebas: habla lo justo del protocolo
 * (EHLO, AUTH PLAIN, MAIL, RCPT, DATA, QUIT), acepta todo y guarda cada
 * mensaje recibido. Sin TLS. Tambien se puede levantar solo (main) para
 * probar el envio desde el backend en vivo.
 */
public final class ServidorSmtpFalso implements AutoCloseable {

    /** Un correo recibido: el usuario que se autentico, los destinatarios y el mensaje MIME completo. */
    public record Recibido(String usuario, List<String> destinatarios, String datos) {
    }

    private final ServerSocket socket;
    private final List<Recibido> recibidos = new CopyOnWriteArrayList<>();
    private volatile boolean rechazarAutenticacion;

    public ServidorSmtpFalso(int puerto) throws IOException {
        socket = new ServerSocket(puerto, 10, InetAddress.getLoopbackAddress());
        Thread.ofVirtual().start(this::aceptar);
    }

    public int puerto() {
        return socket.getLocalPort();
    }

    public List<Recibido> recibidos() {
        return recibidos;
    }

    /** Para probar el camino del error: responde 535 a la autenticacion. */
    public void rechazarAutenticacion() {
        rechazarAutenticacion = true;
    }

    private void aceptar() {
        while (!socket.isClosed()) {
            try {
                Socket cliente = socket.accept();
                Thread.ofVirtual().start(() -> atender(cliente));
            } catch (IOException ex) {
                return;
            }
        }
    }

    private void atender(Socket cliente) {
        try (cliente;
             BufferedReader entrada = new BufferedReader(new InputStreamReader(cliente.getInputStream(), StandardCharsets.UTF_8));
             OutputStream salida = cliente.getOutputStream()) {
            responder(salida, "220 smtp-falso ESMTP");
            String usuario = null;
            List<String> destinatarios = new CopyOnWriteArrayList<>();
            String linea;
            while ((linea = entrada.readLine()) != null) {
                String comando = linea.toUpperCase();
                if (comando.startsWith("EHLO") || comando.startsWith("HELO")) {
                    responder(salida, "250-smtp-falso\r\n250-AUTH PLAIN\r\n250 OK");
                } else if (comando.startsWith("AUTH PLAIN")) {
                    String credencial = linea.length() > 11 ? linea.substring(11).strip() : null;
                    if (credencial == null || credencial.isEmpty()) {
                        responder(salida, "334 ");
                        credencial = entrada.readLine();
                    }
                    String[] partes = new String(Base64.getDecoder().decode(credencial), StandardCharsets.UTF_8).split("\u0000");
                    usuario = partes.length > 1 ? partes[1] : null;
                    responder(salida, rechazarAutenticacion ? "535 5.7.8 Autenticacion rechazada" : "235 2.7.0 OK");
                } else if (comando.startsWith("MAIL FROM")) {
                    responder(salida, "250 OK");
                } else if (comando.startsWith("RCPT TO")) {
                    destinatarios.add(linea.replaceAll("(?i)RCPT TO:\\s*<?([^>]*)>?.*", "$1"));
                    responder(salida, "250 OK");
                } else if (comando.startsWith("DATA")) {
                    responder(salida, "354 Envie el mensaje; termine con un punto");
                    StringBuilder datos = new StringBuilder();
                    String l;
                    while ((l = entrada.readLine()) != null && !l.equals(".")) {
                        datos.append(l.startsWith("..") ? l.substring(1) : l).append("\r\n");
                    }
                    recibidos.add(new Recibido(usuario, List.copyOf(destinatarios), datos.toString()));
                    destinatarios.clear();
                    responder(salida, "250 OK recibido");
                } else if (comando.startsWith("QUIT")) {
                    responder(salida, "221 Adios");
                    return;
                } else {
                    responder(salida, "250 OK");
                }
            }
        } catch (IOException ignorada) {
            // El cliente cerro la conexion
        }
    }

    private static void responder(OutputStream salida, String texto) throws IOException {
        salida.write((texto + "\r\n").getBytes(StandardCharsets.UTF_8));
        salida.flush();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    /** Para probar el backend en vivo: java -cp ... ServidorSmtpFalso 2525 (imprime cada correo recibido). */
    public static void main(String[] args) throws Exception {
        ServidorSmtpFalso servidor = new ServidorSmtpFalso(args.length > 0 ? Integer.parseInt(args[0]) : 2525);
        System.out.println("SMTP falso escuchando en el puerto " + servidor.puerto());
        int vistos = 0;
        while (true) {
            Thread.sleep(500);
            while (vistos < servidor.recibidos().size()) {
                Recibido r = servidor.recibidos().get(vistos++);
                String asunto = r.datos().lines().filter(l -> l.startsWith("Subject:")).findFirst().orElse("Subject: ?");
                long adjuntos = r.datos().lines().filter(l -> l.contains("filename=")).count();
                System.out.println("CORREO para " + r.destinatarios() + " | " + asunto + " | adjuntos: " + adjuntos
                        + " | usuario autenticado: " + r.usuario() + " | " + r.datos().length() + " bytes");
            }
        }
    }
}
