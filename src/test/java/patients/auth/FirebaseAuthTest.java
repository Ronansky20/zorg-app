package patients.auth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import patients.testing.MutableClock;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FirebaseAuthTest {

    private HttpServer server;
    private final List<String> requests = new ArrayList<>();
    private final List<String> bodies = new ArrayList<>();
    private int status = 200;
    private String signInResponse = """
            {"localId":"jansen","idToken":"id-1","refreshToken":"refresh-1","expiresIn":"3600"}
            """;
    private String refreshResponse = """
            {"user_id":"jansen","id_token":"id-2","refresh_token":"refresh-2","expires_in":"3600"}
            """;
    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-13T10:00:00Z"));
    private FirebaseAuth auth;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
        auth = new FirebaseAuth("http://127.0.0.1:" + server.getAddress().getPort(), "test-key", clock);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI());
        bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        String body = exchange.getRequestURI().getPath().contains("securetoken") ? refreshResponse : signInResponse;
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    @Test
    void signInReturnsTheUidAndKeepsTheIdToken() {
        assertEquals("jansen", auth.signIn("jansen@zorg.test", "secret"));

        assertEquals("id-1", auth.idToken());
        assertEquals("POST /identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=test-key",
                requests.get(0));
        assertTrue(bodies.get(0).contains("\"email\":\"jansen@zorg.test\""), bodies.get(0));
        assertTrue(bodies.get(0).contains("\"returnSecureToken\":true"), bodies.get(0));
    }

    @Test
    void noTokenBeforeSigningIn() {
        assertNull(auth.idToken());
        assertTrue(requests.isEmpty());
    }

    @Test
    void tokenIsReusedUntilShortlyBeforeItExpires() {
        auth.signIn("jansen@zorg.test", "secret");

        clock.advance(Duration.ofMinutes(58));

        assertEquals("id-1", auth.idToken());
        assertEquals(1, requests.size());
    }

    @Test
    void expiringTokenIsRefreshed() {
        auth.signIn("jansen@zorg.test", "secret");

        clock.advance(Duration.ofMinutes(59));

        assertEquals("id-2", auth.idToken());
        assertEquals("POST /securetoken.googleapis.com/v1/token?key=test-key", requests.get(1));
        assertEquals("grant_type=refresh_token&refresh_token=refresh-1", bodies.get(1));
    }

    @Test
    void refreshUsesTheNewestRefreshToken() {
        auth.signIn("jansen@zorg.test", "secret");
        clock.advance(Duration.ofMinutes(59));
        auth.idToken();

        clock.advance(Duration.ofMinutes(59));
        auth.idToken();

        assertEquals("grant_type=refresh_token&refresh_token=refresh-2", bodies.get(2));
    }

    @Test
    void wrongPasswordIsASecurityException() {
        status = 400;
        signInResponse = "{\"error\":{\"code\":400,\"message\":\"INVALID_PASSWORD\"}}";

        SecurityException e = assertThrows(SecurityException.class, () -> auth.signIn("jansen@zorg.test", "nope"));
        assertEquals("Wrong email or password", e.getMessage());
        assertNull(auth.idToken());
    }

    @Test
    void invalidLoginCredentialsWithDetailsIsASecurityException() {
        status = 400;
        signInResponse = "{\"error\":{\"code\":400,\"message\":\"INVALID_LOGIN_CREDENTIALS : details\"}}";

        assertThrows(SecurityException.class, () -> auth.signIn("jansen@zorg.test", "nope"));
    }

    @Test
    void tooManyAttemptsIsASecurityException() {
        status = 400;
        signInResponse = "{\"error\":{\"code\":400,\"message\":\"TOO_MANY_ATTEMPTS_TRY_LATER : Access disabled\"}}";

        SecurityException e = assertThrows(SecurityException.class, () -> auth.signIn("jansen@zorg.test", "nope"));
        assertEquals("Too many failed attempts, try again later", e.getMessage());
    }

    @Test
    void otherErrorsAreUncheckedIOExceptions() {
        status = 400;
        signInResponse = "{\"error\":{\"code\":400,\"message\":\"API_KEY_INVALID\"}}";

        UncheckedIOException e = assertThrows(UncheckedIOException.class,
                () -> auth.signIn("jansen@zorg.test", "secret"));
        assertTrue(e.getMessage().contains("API_KEY_INVALID"), e.getMessage());
    }

    @Test
    void unreachableServerIsAnUncheckedIOException() {
        server.stop(0);

        assertThrows(UncheckedIOException.class, () -> auth.signIn("jansen@zorg.test", "secret"));
    }

    @Test
    void responseWithoutATokenIsAnUncheckedIOException() {
        signInResponse = "{\"localId\":\"jansen\"}";

        assertThrows(UncheckedIOException.class, () -> auth.signIn("jansen@zorg.test", "secret"));
        assertNull(auth.idToken());
    }
}
