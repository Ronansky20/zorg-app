package patients;

import patients.auth.FirebaseAuth;
import patients.repository.FirebaseClient;
import patients.repository.FirebasePatientRepository;
import patients.repository.FirebaseUserRepository;
import patients.repository.JsonPatientRepository;
import patients.repository.JsonUserRepository;
import patients.repository.PatientRepository;
import patients.repository.UserRepository;

import java.net.URI;
import java.nio.file.Path;
import java.util.function.Function;

public record Backend(String firebaseUrl, String authEmulatorHost, String apiKey) {

    public static final String SIGN_IN_SETTINGS_MISSING = "Set FIREBASE_AUTH_EMULATOR_HOST (emulator) or"
            + " ZORG_FIREBASE_API_KEY (real project) to sign in, see the README";

    public record Repositories(PatientRepository patients, UserRepository users) {}

    public static Backend fromEnvironment(Function<String, String> env) {
        return new Backend(
                blankToNull(env.apply("ZORG_FIREBASE_URL")),
                blankToNull(env.apply("FIREBASE_AUTH_EMULATOR_HOST")),
                blankToNull(env.apply("ZORG_FIREBASE_API_KEY")));
    }

    public boolean usesFirebase() {
        return firebaseUrl != null;
    }

    public boolean canSignIn() {
        return authEmulatorHost != null || apiKey != null;
    }

    public FirebaseAuth newAuth() {
        if (authEmulatorHost != null) {
            return FirebaseAuth.forEmulator(authEmulatorHost);
        }
        if (apiKey != null) {
            return FirebaseAuth.forProject(apiKey);
        }
        throw new IllegalStateException(SIGN_IN_SETTINGS_MISSING);
    }

    public Repositories firebase(FirebaseAuth auth) {
        FirebaseClient client = new FirebaseClient(URI.create(firebaseUrl), auth::idToken);
        return new Repositories(new FirebasePatientRepository(client), new FirebaseUserRepository(client));
    }

    public static Repositories json(Path dataDir) {
        return new Repositories(
                new JsonPatientRepository(dataDir.resolve("patients.json")),
                new JsonUserRepository(dataDir.resolve("users.json")));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
