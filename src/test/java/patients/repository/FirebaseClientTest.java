package patients.repository;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FirebaseClientTest {

    private FakeRealtimeDatabase database;

    @BeforeEach
    void setUp() throws IOException {
        database = new FakeRealtimeDatabase();
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    @Test
    void uriAddsPathJsonSuffixAndKeepsTheNamespaceQuery() {
        FirebaseClient client = new FirebaseClient(URI.create("http://127.0.0.1:9000/?ns=demo"), () -> null);

        assertEquals(URI.create("http://127.0.0.1:9000/patients/a1.json?ns=demo"), client.uriFor("patients/a1"));
        assertEquals(URI.create("http://127.0.0.1:9000/.json?ns=demo"), client.uriFor(""));
    }

    @Test
    void uriAddsTheAuthToken() {
        FirebaseClient client = new FirebaseClient(URI.create("https://db.example.com"), () -> "secret");

        assertEquals(URI.create("https://db.example.com/users.json?auth=secret"), client.uriFor("users"));
    }

    @Test
    void uriEncodesKeysThatAreNotUrlSafe() {
        FirebaseClient client = new FirebaseClient(URI.create("https://db.example.com"), () -> null);

        assertEquals("/users/dr%20jansen.json", client.uriFor("users/dr jansen").getRawPath());
    }

    @Test
    void putThenGetReturnsTheStoredValue() {
        FirebaseClient client = new FirebaseClient(database.url(), () -> null);
        JsonObject value = JsonParser.parseString("{\"name\":\"Dr. Max Jansen\"}").getAsJsonObject();

        client.put("users/jansen", value);

        assertEquals(value, client.get("users/jansen"));
    }

    @Test
    void getOfAMissingPathIsJsonNull() {
        FirebaseClient client = new FirebaseClient(database.url(), () -> null);

        assertTrue(client.get("users/nobody").isJsonNull());
    }

    @Test
    void patchAtTheRootWritesEveryPath() {
        FirebaseClient client = new FirebaseClient(database.url(), () -> null);
        JsonObject update = JsonParser.parseString("{\"a/b\":1,\"c\":true}").getAsJsonObject();

        client.patch("", update);

        assertEquals(1, database.get("a/b").getAsInt());
        assertTrue(database.get("c").getAsBoolean());
    }

    @Test
    void errorStatusBecomesUncheckedIOExceptionWithTheResponse() {
        FirebaseClient client = new FirebaseClient(database.url(), () -> null);
        database.failWithStatus = 401;

        UncheckedIOException e = assertThrows(UncheckedIOException.class, () -> client.get("patients"));
        assertTrue(e.getMessage().contains("HTTP 401"), e.getMessage());
        assertTrue(e.getMessage().contains("Permission denied"), e.getMessage());
    }

    @Test
    void unreachableDatabaseBecomesUncheckedIOException() {
        URI url = database.url();
        database.close();
        FirebaseClient client = new FirebaseClient(url, () -> null);

        assertThrows(UncheckedIOException.class, () -> client.get("patients"));
    }

    @Test
    void tokenIsReadOnEveryRequest() {
        String[] token = {"first"};
        FirebaseClient client = new FirebaseClient(database.url(), () -> token[0]);

        client.get("patients");
        token[0] = "second";
        client.get("patients");

        assertTrue(database.requests.get(0).endsWith("auth=first"), database.requests.get(0));
        assertTrue(database.requests.get(1).endsWith("auth=second"), database.requests.get(1));
    }

    @Test
    void adminClientSendsTheTokenAsBearerHeaderNotInTheUrl() {
        FirebaseClient client = FirebaseClient.admin(database.url(), "owner");

        client.get("patients");

        assertEquals("Bearer owner", database.authorizations.get(0));
        assertFalse(database.requests.get(0).contains("auth="), database.requests.get(0));
    }

    @Test
    void userClientSendsNoAuthorizationHeader() {
        FirebaseClient client = new FirebaseClient(database.url(), () -> "id-token");

        client.get("patients");

        assertEquals("null", database.authorizations.get(0));
    }
}
