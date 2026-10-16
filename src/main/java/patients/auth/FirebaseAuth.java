package patients.auth;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;

public class FirebaseAuth implements Authenticator {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final Duration REFRESH_MARGIN = Duration.ofMinutes(1);
    private static final Set<String> WRONG_CREDENTIALS = Set.of("EMAIL_NOT_FOUND", "INVALID_PASSWORD",
            "INVALID_LOGIN_CREDENTIALS", "INVALID_EMAIL", "MISSING_PASSWORD", "USER_DISABLED");
    private static final String TOO_MANY_ATTEMPTS = "TOO_MANY_ATTEMPTS_TRY_LATER";

    private final String endpointPrefix;
    private final String apiKey;
    private final Clock clock;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    private String idToken;
    private String refreshToken;
    private Instant refreshAt;

    public FirebaseAuth(String endpointPrefix, String apiKey) {
        this(endpointPrefix, apiKey, Clock.systemUTC());
    }

    FirebaseAuth(String endpointPrefix, String apiKey, Clock clock) {
        this.endpointPrefix = endpointPrefix.endsWith("/") ? endpointPrefix : endpointPrefix + "/";
        this.apiKey = apiKey;
        this.clock = clock;
    }

    public static FirebaseAuth forEmulator(String host) {
        return new FirebaseAuth("http://" + host + "/", "demo-api-key");
    }

    public static FirebaseAuth forProject(String apiKey) {
        return new FirebaseAuth("https://", apiKey);
    }

    @Override
    public synchronized String signIn(String email, String password) {
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", password);
        body.addProperty("returnSecureToken", true);
        JsonObject response = post("identitytoolkit.googleapis.com/v1/accounts:signInWithPassword",
                "application/json", body.toString());
        store(response, "idToken", "refreshToken", "expiresIn");
        return string(response, "localId");
    }

    public synchronized String idToken() {
        if (idToken != null && !clock.instant().isBefore(refreshAt)) {
            refresh();
        }
        return idToken;
    }

    private void refresh() {
        String form = "grant_type=refresh_token&refresh_token=" + URLEncoder.encode(refreshToken, UTF_8);
        JsonObject response = post("securetoken.googleapis.com/v1/token",
                "application/x-www-form-urlencoded", form);
        store(response, "id_token", "refresh_token", "expires_in");
    }

    private void store(JsonObject response, String idTokenKey, String refreshTokenKey, String expiresInKey) {
        String newIdToken = string(response, idTokenKey);
        String newRefreshToken = string(response, refreshTokenKey);
        long expiresIn;
        try {
            expiresIn = Long.parseLong(string(response, expiresInKey));
        } catch (NumberFormatException e) {
            throw failure("Firebase Auth returned an invalid " + expiresInKey, e);
        }
        idToken = newIdToken;
        refreshToken = newRefreshToken;
        refreshAt = clock.instant().plusSeconds(expiresIn).minus(REFRESH_MARGIN);
    }

    private JsonObject post(String endpoint, String contentType, String body) {
        URI uri = URI.create(endpointPrefix + endpoint + "?key=" + URLEncoder.encode(apiKey, UTF_8));
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(TIMEOUT)
                .header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw failure("Firebase Auth request failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UncheckedIOException(new InterruptedIOException("Firebase Auth request was interrupted"));
        }

        if (response.statusCode() / 100 != 2) {
            String code = errorCode(response.body());
            if (WRONG_CREDENTIALS.contains(code)) {
                throw new SecurityException("Wrong email or password");
            }
            if (TOO_MANY_ATTEMPTS.equals(code)) {
                throw new SecurityException("Too many failed attempts, try again later");
            }
            throw failure("Firebase Auth failed: HTTP " + response.statusCode() + " " + response.body(), null);
        }
        try {
            JsonElement json = JsonParser.parseString(response.body());
            if (json.isJsonObject()) {
                return json.getAsJsonObject();
            }
        } catch (JsonParseException e) {
            throw failure("Firebase Auth returned invalid JSON", e);
        }
        throw failure("Firebase Auth returned invalid JSON", null);
    }

    private static String errorCode(String body) {
        try {
            String message = JsonParser.parseString(body).getAsJsonObject()
                    .getAsJsonObject("error").get("message").getAsString();
            return message.split("[ :]", 2)[0];
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static String string(JsonObject response, String key) {
        JsonElement value = response.get(key);
        if (value == null || !value.isJsonPrimitive()) {
            throw failure("Firebase Auth response has no " + key, null);
        }
        return value.getAsString();
    }

    private static UncheckedIOException failure(String message, Exception cause) {
        return new UncheckedIOException(message, new IOException(message, cause));
    }
}
