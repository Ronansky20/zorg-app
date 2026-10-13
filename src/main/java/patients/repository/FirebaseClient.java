package patients.repository;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Supplier;

public class FirebaseClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final URI databaseUrl;
    private final Supplier<String> authToken;
    private final String adminToken;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private final Gson gson = GsonFactory.builder().serializeNulls().create();

    public FirebaseClient(URI databaseUrl, Supplier<String> authToken) {
        this(databaseUrl, authToken, null);
    }

    private FirebaseClient(URI databaseUrl, Supplier<String> authToken, String adminToken) {
        this.databaseUrl = databaseUrl;
        this.authToken = authToken;
        this.adminToken = adminToken;
    }

    public static FirebaseClient admin(URI databaseUrl, String adminToken) {
        return new FirebaseClient(databaseUrl, () -> null, adminToken);
    }

    public JsonElement get(String path) {
        return send("GET", path, null);
    }

    public void put(String path, JsonElement body) {
        send("PUT", path, body);
    }

    public void patch(String path, JsonElement body) {
        send("PATCH", path, body);
    }

    private JsonElement send(String method, String path, JsonElement body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uriFor(path))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .method(method, body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(gson.toJson(body)));
        if (adminToken != null && !adminToken.isBlank()) {
            builder.header("Authorization", "Bearer " + adminToken);
        }
        HttpRequest request = builder.build();
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new UncheckedIOException("Firebase " + method + " /" + path + " failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UncheckedIOException(new InterruptedIOException("Firebase " + method + " /" + path + " was interrupted"));
        }

        if (response.statusCode() / 100 != 2) {
            String message = "Firebase " + method + " /" + path + " failed: HTTP " + response.statusCode()
                    + " " + response.body();
            throw new UncheckedIOException(message, new IOException(message));
        }
        try {
            return JsonParser.parseString(response.body());
        } catch (JsonParseException e) {
            String message = "Firebase " + method + " /" + path + " returned invalid JSON";
            throw new UncheckedIOException(message, new IOException(message, e));
        }
    }

    URI uriFor(String path) {
        String basePath = databaseUrl.getPath() == null ? "" : databaseUrl.getPath().replaceAll("/+$", "");
        String query = databaseUrl.getQuery();
        String token = authToken.get();
        if (token != null && !token.isBlank()) {
            query = (query == null || query.isEmpty() ? "" : query + "&") + "auth=" + token;
        }
        try {
            return new URI(databaseUrl.getScheme(), databaseUrl.getAuthority(),
                    basePath + "/" + path + ".json", query, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid Firebase path: " + path, e);
        }
    }
}
