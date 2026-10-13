package patients;

import patients.model.Patient;
import patients.model.User;
import patients.repository.FirebaseClient;
import patients.repository.FirebasePatientRepository;
import patients.repository.FirebaseUserRepository;
import patients.repository.JsonPatientRepository;
import patients.repository.JsonUserRepository;
import patients.repository.PatientRepository;
import patients.repository.UserRepository;

import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MigrateToFirebase {

    record Result(int users, int patients, List<String> warnings) {}

    public static void main(String[] args) {
        String firebaseUrl = System.getenv("ZORG_FIREBASE_URL");
        if (firebaseUrl == null || firebaseUrl.isBlank()) {
            System.err.println("Set ZORG_FIREBASE_URL to the database to migrate to, see the README");
            System.exit(1);
        }
        String adminToken = System.getenv("ZORG_FIREBASE_ADMIN_TOKEN");
        if (adminToken == null || adminToken.isBlank()) {
            System.err.println("Set ZORG_FIREBASE_ADMIN_TOKEN (the emulator accepts 'owner'), see the README");
            System.exit(1);
        }
        Path usersFile = Paths.get("users.json");
        Path patientsFile = Paths.get("patients.json");
        if (!Files.exists(usersFile)) {
            System.err.println("No " + usersFile.toAbsolutePath() + "; run this from the project root");
            System.exit(1);
        }
        if (!Files.exists(patientsFile)) {
            System.out.println("No " + patientsFile.toAbsolutePath() + "; only the users are migrated");
        }

        FirebaseClient client = FirebaseClient.admin(URI.create(firebaseUrl), adminToken);
        System.out.println("Migrating to Firebase at " + firebaseUrl);
        try {
            Result result = migrate(new JsonUserRepository(usersFile), new JsonPatientRepository(patientsFile),
                    new FirebaseUserRepository(client), new FirebasePatientRepository(client));
            result.warnings().forEach(w -> System.out.println("Warning: " + w));
            System.out.println("Migrated " + result.users() + " users and " + result.patients() + " patients");
        } catch (UncheckedIOException e) {
            System.err.println("Migration failed: " + e.getMessage());
            System.exit(1);
        }
    }

    static Result migrate(UserRepository fromUsers, PatientRepository fromPatients,
                          FirebaseUserRepository toUsers, PatientRepository toPatients) {
        List<User> users = fromUsers.findAll();
        List<Patient> patients = fromPatients.findAll();
        Set<String> userIds = users.stream().map(User::id).collect(Collectors.toSet());

        List<String> warnings = new ArrayList<>();
        for (Patient patient : patients) {
            for (String member : patient.careTeam().keySet()) {
                if (!userIds.contains(member)) {
                    warnings.add("patient " + patient.id() + " (" + patient.firstName() + " " + patient.lastName()
                            + ") has unknown care team member '" + member + "'");
                }
            }
        }

        users.forEach(toUsers::save);
        patients.forEach(toPatients::save);
        return new Result(users.size(), patients.size(), warnings);
    }
}
