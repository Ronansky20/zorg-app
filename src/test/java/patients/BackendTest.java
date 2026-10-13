package patients;

import org.junit.jupiter.api.Test;

import patients.repository.FirebasePatientRepository;
import patients.repository.JsonPatientRepository;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackendTest {

    private static Backend backend(Map<String, String> env) {
        return Backend.fromEnvironment(env::get);
    }

    @Test
    void withoutAFirebaseUrlTheJsonFilesAreUsed() {
        Backend backend = backend(Map.of("ZORG_FIREBASE_URL", " "));

        assertFalse(backend.usesFirebase());
        assertInstanceOf(JsonPatientRepository.class, Backend.json(Path.of("")).patients());
    }

    @Test
    void theEmulatorHostWinsOverAnApiKey() {
        Backend backend = backend(Map.of(
                "ZORG_FIREBASE_URL", "http://127.0.0.1:9000?ns=demo",
                "FIREBASE_AUTH_EMULATOR_HOST", "127.0.0.1:9099",
                "ZORG_FIREBASE_API_KEY", "key"));

        assertTrue(backend.usesFirebase());
        assertTrue(backend.canSignIn());
        assertEquals("127.0.0.1:9099", backend.authEmulatorHost());
        assertInstanceOf(FirebasePatientRepository.class, backend.firebase(backend.newAuth()).patients());
    }

    @Test
    void anApiKeyIsEnoughToSignIn() {
        Backend backend = backend(Map.of("ZORG_FIREBASE_URL", "https://example.test", "ZORG_FIREBASE_API_KEY", "key"));

        assertTrue(backend.canSignIn());
        assertNotNull(backend.newAuth());
    }

    @Test
    void firebaseWithoutSignInSettingsCanNotCreateAnAuthenticator() {
        Backend backend = backend(Map.of("ZORG_FIREBASE_URL", "https://example.test"));

        assertFalse(backend.canSignIn());
        IllegalStateException e = assertThrows(IllegalStateException.class, backend::newAuth);
        assertEquals(Backend.SIGN_IN_SETTINGS_MISSING, e.getMessage());
    }
}
