package patients.repository;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** In-memory stand-in for the Firebase Realtime Database REST API: GET, PUT and (multi-path) PATCH. */
class FakeRealtimeDatabase implements AutoCloseable {

    private final HttpServer server;
    private JsonObject root = new JsonObject();
    final List<String> requests = new ArrayList<>();
    int failWithStatus = 0;

    FakeRealtimeDatabase() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    URI url() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    synchronized JsonElement get(String path) {
        JsonElement node = root;
        for (String key : segments(path)) {
            if (!node.isJsonObject() || !node.getAsJsonObject().has(key)) {
                return JsonNull.INSTANCE;
            }
            node = node.getAsJsonObject().get(key);
        }
        return node.deepCopy();
    }

    synchronized void set(String path, JsonElement value) {
        List<String> keys = segments(path);
        if (keys.isEmpty()) {
            root = value != null && value.isJsonObject() ? value.getAsJsonObject().deepCopy() : new JsonObject();
            prune(root);
            return;
        }
        JsonObject parent = root;
        for (String key : keys.subList(0, keys.size() - 1)) {
            if (!parent.has(key) || !parent.get(key).isJsonObject()) {
                parent.add(key, new JsonObject());
            }
            parent = parent.getAsJsonObject(key);
        }
        String last = keys.get(keys.size() - 1);
        if (value == null || value.isJsonNull()) {
            parent.remove(last);
        } else {
            parent.add(last, value.deepCopy());
        }
        prune(root);
    }

    private synchronized void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        URI uri = exchange.getRequestURI();
        requests.add(method + " " + uri);
        if (failWithStatus != 0) {
            respond(exchange, failWithStatus, "{\"error\":\"Permission denied\"}");
            return;
        }
        String path = uri.getPath().replaceFirst("\\.json$", "");
        String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

        switch (method) {
            case "GET" -> respond(exchange, 200, get(path).toString());
            case "PUT" -> {
                set(path, JsonParser.parseString(requestBody));
                respond(exchange, 200, requestBody);
            }
            case "PATCH" -> {
                JsonObject update = JsonParser.parseString(requestBody).getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : update.entrySet()) {
                    set(path + "/" + entry.getKey(), entry.getValue());
                }
                respond(exchange, 200, requestBody);
            }
            default -> respond(exchange, 405, "{\"error\":\"Method not allowed\"}");
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    // The real database never stores empty objects; removing the last child removes the parent too.
    private static boolean prune(JsonObject object) {
        for (String key : new ArrayList<>(object.keySet())) {
            JsonElement child = object.get(key);
            if (child.isJsonNull() || (child.isJsonObject() && prune(child.getAsJsonObject()))) {
                object.remove(key);
            }
        }
        return object.isEmpty();
    }

    private static List<String> segments(String path) {
        return Arrays.stream(path.split("/")).filter(s -> !s.isEmpty()).toList();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
