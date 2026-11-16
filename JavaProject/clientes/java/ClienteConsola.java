import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/** Cliente independiente: no importa dominio, contratos ni persistencia. */
public final class ClienteConsola {
    public static void main(String[] args) {
        if (args.length < 3) { System.err.println("Uso: ClienteConsola token GET|POST /api/ruta [campo=valor ...]"); System.exit(1); }
        try {
            String base = System.getProperty("polimarket.url", "http://127.0.0.1:8080");
            if (!Set.of("GET", "POST").contains(args[1]) || !args[2].startsWith("/api/")) throw new IllegalArgumentException("Método o ruta inválida");
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(base + args[2])).timeout(Duration.ofSeconds(10)).header("Authorization", "Bearer " + args[0]);
            if (args[1].equals("POST")) {
                List<String> parts = new ArrayList<>();
                for (int i = 3; i < args.length; i++) {
                    String[] pair = args[i].split("=", 2);
                    if (pair.length != 2) throw new IllegalArgumentException("Use campo=valor");
                    parts.add(URLEncoder.encode(pair[0], StandardCharsets.UTF_8) + "=" + URLEncoder.encode(pair[1], StandardCharsets.UTF_8));
                }
                request.header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(String.join("&", parts)));
            } else request.GET();
            try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
                var response = client.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                System.out.println("HTTP " + response.statusCode()); System.out.println(response.body());
                if (response.statusCode() >= 400) System.exit(2);
            }
        } catch (Exception e) { System.err.println("No se pudo completar la solicitud: " + e.getMessage()); System.exit(1); }
    }
}
