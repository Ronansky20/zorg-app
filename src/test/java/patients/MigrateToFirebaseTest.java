package patients;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import patients.model.Medication;
import patients.model.Patient;
import patients.model.Profession;
import patients.model.User;
import patients.repository.FakeRealtimeDatabase;
import patients.repository.FirebaseClient;
import patients.repository.FirebasePatientRepository;
import patients.repository.FirebaseUserRepository;
import patients.repository.JsonPatientRepository;
import patients.repository.JsonUserRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigrateToFirebaseTest {

    @TempDir
    Path tempDir;

    private FakeRealtimeDatabase database;
    private JsonUserRepository jsonUsers;
    private JsonPatientRepository jsonPatients;
    private FirebaseUserRepository firebaseUsers;
    private FirebasePatientRepository firebasePatients;

    private static Patient patient(String id, String firstName, Map<String, Boolean> careTeam) {
        return new Patient(id, firstName, "Doe", LocalDate.of(1995, 6, 15), 70.0, 1.8, "Elm Street 58",
                careTeam, List.of(new Medication("Paracetamol", "500 mg", "3x daily")));
    }

    @BeforeEach
    void setUp() throws IOException {
        Path usersFile = tempDir.resolve("users.json");
        Files.writeString(usersFile, """
                {
                  "users": {
                    "jansen": { "name": "Dr. Max Jansen", "profession": "DOCTOR" },
                    "devries": { "name": "Pieter de Vries", "profession": "PHARMACIST" }
                  }
                }
                """);
        jsonUsers = new JsonUserRepository(usersFile);
        jsonPatients = new JsonPatientRepository(tempDir.resolve("patients.json"));

        database = new FakeRealtimeDatabase();
        FirebaseClient client = new FirebaseClient(database.url(), () -> null);
        firebaseUsers = new FirebaseUserRepository(client);
        firebasePatients = new FirebasePatientRepository(client);
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    private MigrateToFirebase.Result migrate() {
        return MigrateToFirebase.migrate(jsonUsers, jsonPatients, firebaseUsers, firebasePatients);
    }

    @Test
    void copiesEveryUserAndPatient() {
        Patient john = patient("a1", "John", Map.of("jansen", true, "devries", true));
        Patient jane = patient("b2", "Jane", Map.of("jansen", true));
        jsonPatients.save(john);
        jsonPatients.save(jane);

        MigrateToFirebase.Result result = migrate();

        assertEquals(2, result.users());
        assertEquals(2, result.patients());
        assertTrue(result.warnings().isEmpty());
        assertEquals(Optional.of(new User("jansen", "Dr. Max Jansen", Profession.DOCTOR)),
                firebaseUsers.findById("jansen"));
        assertEquals(Optional.of(new User("devries", "Pieter de Vries", Profession.PHARMACIST)),
                firebaseUsers.findById("devries"));
        assertEquals(Optional.of(john), firebasePatients.findById("a1"));
        assertEquals(Optional.of(jane), firebasePatients.findById("b2"));
    }

    @Test
    void buildsTheUserPatientsIndex() {
        jsonPatients.save(patient("a1", "John", Map.of("jansen", true, "devries", true)));
        jsonPatients.save(patient("b2", "Jane", Map.of("jansen", true)));

        migrate();

        assertEquals(JsonParser.parseString("{ \"a1\": true, \"b2\": true }"), database.get("userPatients/jansen"));
        assertEquals(JsonParser.parseString("{ \"a1\": true }"), database.get("userPatients/devries"));
    }

    @Test
    void runningTwiceGivesTheSameData() {
        jsonPatients.save(patient("a1", "John", Map.of("jansen", true, "devries", true)));
        migrate();
        JsonElement afterFirstRun = database.get("");

        migrate();

        assertEquals(afterFirstRun, database.get(""));
    }

    @Test
    void rerunAfterATeamChangeRemovesTheOldIndexEntry() {
        jsonPatients.save(patient("a1", "John", Map.of("jansen", true, "devries", true)));
        migrate();

        jsonPatients.save(patient("a1", "John", Map.of("jansen", true)));
        migrate();

        assertTrue(database.get("userPatients/devries").isJsonNull());
        assertEquals(List.of(), firebasePatients.findAllForUser("devries"));
    }

    @Test
    void leavesRecordsThatOnlyExistInFirebase() {
        database.set("patients/zz/firstName", new JsonPrimitive("Only in Firebase"));

        migrate();

        assertEquals("Only in Firebase", database.get("patients/zz/firstName").getAsString());
    }

    @Test
    void warnsAboutUnknownCareTeamMembers() {
        jsonPatients.save(patient("a1", "John", Map.of("jansen", true, "ghost", true)));

        MigrateToFirebase.Result result = migrate();

        assertEquals(List.of("patient a1 (John Doe) has unknown care team member 'ghost'"), result.warnings());
        assertEquals(1, result.patients());
    }

    @Test
    void emptyPatientsFileOnlyMigratesUsers() {
        MigrateToFirebase.Result result = migrate();

        assertEquals(2, result.users());
        assertEquals(0, result.patients());
        assertTrue(database.get("patients").isJsonNull());
    }
}
