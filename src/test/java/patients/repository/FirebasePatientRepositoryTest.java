package patients.repository;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import patients.model.Medication;
import patients.model.Patient;
import patients.model.PatientField;

import java.io.IOException;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FirebasePatientRepositoryTest {

    private FakeRealtimeDatabase database;
    private FirebasePatientRepository repository;

    private static Patient patient(String id, String firstName, Map<String, Boolean> careTeam) {
        return new Patient(id, firstName, "Doe", LocalDate.of(1995, 6, 15), 70.0, 1.8, "Elm Street 58",
                careTeam, List.of(new Medication("Paracetamol", "500 mg", "3x daily")));
    }

    @BeforeEach
    void setUp() throws IOException {
        database = new FakeRealtimeDatabase();
        repository = new FirebasePatientRepository(new FirebaseClient(database.url(), () -> null));
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    @Test
    void savedPatientCanBeFoundById() {
        Patient john = patient("a1", "John", Map.of("jansen", true));

        repository.save(john);

        assertEquals(Optional.of(john), repository.findById("a1"));
        assertEquals(Optional.empty(), repository.findById("nope"));
    }

    @Test
    void patientIsStoredUnderItsIdWithoutAnIdField() {
        repository.save(patient("a1", "John", Map.of("jansen", true)));

        JsonObject stored = database.get("patients/a1").getAsJsonObject();
        assertEquals("John", stored.get("firstName").getAsString());
        assertEquals("1995-06-15", stored.get("birthDate").getAsString());
        assertFalse(stored.has("id"));
    }

    @Test
    void saveIndexesThePatientForEveryCareTeamMember() {
        repository.save(patient("a1", "John", Map.of("jansen", true, "devries", true, "visser", false)));

        assertTrue(database.get("userPatients/jansen/a1").getAsBoolean());
        assertTrue(database.get("userPatients/devries/a1").getAsBoolean());
        assertTrue(database.get("userPatients/visser").isJsonNull());
    }

    @Test
    void removingSomeoneFromTheTeamRemovesTheirIndexEntry() {
        repository.save(patient("a1", "John", Map.of("jansen", true, "devries", true)));

        repository.save(patient("a1", "John", Map.of("jansen", true)));

        assertTrue(database.get("userPatients/jansen/a1").getAsBoolean());
        assertTrue(database.get("userPatients/devries").isJsonNull());
    }

    @Test
    void newPatientIsWrittenAsAWholeInOnePatch() {
        repository.save(patient("a1", "John", Map.of("jansen", true)));

        List<String> patches = database.requests.stream().filter(r -> r.startsWith("PATCH")).toList();
        assertEquals(1, patches.size());
        assertEquals("John", database.get("patients/a1/firstName").getAsString());
    }

    @Test
    void editingOneFieldOnlyWritesThatField() {
        Patient john = patient("a1", "John", Map.of("jansen", true));
        repository.save(john);
        database.requests.clear();

        repository.save(john.withAddress("Oak Lane 1"));

        assertEquals(Set.of("patients/a1/address", "userPatients/jansen/a1"), lastPatchPaths());
        assertEquals(Optional.of(john.withAddress("Oak Lane 1")), repository.findById("a1"));
    }

    @Test
    void savingAnUnchangedPatientOnlyRewritesTheIndex() {
        Patient john = patient("a1", "John", Map.of("jansen", true, "devries", true));
        repository.save(john);

        repository.save(john);

        assertEquals(Set.of("userPatients/jansen/a1", "userPatients/devries/a1"), lastPatchPaths());
    }

    @Test
    void wholeNumbersStoredByTheDatabaseCountAsUnchanged() {
        repository.save(patient("a1", "John", Map.of("jansen", true)));
        database.set("patients/a1/weight", new JsonPrimitive(70));

        repository.save(patient("a1", "John", Map.of("jansen", true)));

        assertFalse(lastPatchPaths().contains("patients/a1/weight"));
    }

    @Test
    void changedMedicationsAreWrittenAsOneList() {
        Patient john = patient("a1", "John", Map.of("jansen", true));
        repository.save(john);

        repository.save(john.withMedications(List.of()));

        assertTrue(lastPatchPaths().contains("patients/a1/medications"));
        assertEquals(List.of(), repository.findById("a1").orElseThrow().medications());
    }

    @Test
    void clearedFieldIsRemoved() {
        Patient john = patient("a1", "John", Map.of("jansen", true));
        repository.save(john);

        repository.save(john.withAddress(null));

        assertTrue(database.get("patients/a1/address").isJsonNull());
        assertTrue(lastPatchPaths().contains("patients/a1/address"));
    }

    private Set<String> lastPatchPaths() {
        String patch = database.requests.stream().filter(r -> r.startsWith("PATCH")).reduce((a, b) -> b).orElseThrow();
        assertTrue(patch.startsWith("PATCH /.json"), patch);
        return lastPatchBody().keySet();
    }

    private JsonObject lastPatchBody() {
        return database.patchBodies.get(database.patchBodies.size() - 1);
    }

    @Test
    void findAllForUserReadsOnlyTheirPatients() {
        Patient john = patient("a1", "John", Map.of("jansen", true));
        Patient jane = patient("b2", "Jane", Map.of("hendriks", true));
        repository.save(john);
        repository.save(jane);

        assertEquals(List.of(john), repository.findAllForUser("jansen"));
        assertEquals(List.of(jane), repository.findAllForUser("hendriks"));
        assertEquals(List.of(), repository.findAllForUser("nobody"));
        assertTrue(database.requests.stream().noneMatch(r -> r.startsWith("GET /patients.json")));
    }

    @Test
    void readingSomeFieldsFetchesOnlyThoseFieldsAndTheCareTeam() {
        Patient john = patient("a1", "John", Map.of("devries", true));
        repository.save(john);
        database.requests.clear();

        Patient seen = repository.findAllForUser("devries",
                EnumSet.of(PatientField.FIRST_NAME, PatientField.MEDICATION)).get(0);

        assertEquals("John", seen.firstName());
        assertEquals(john.medications(), seen.medications());
        assertNull(seen.address());
        assertEquals(0.0, seen.weight());
        assertEquals(List.of("GET /userPatients/devries.json", "GET /patients/a1/careTeam.json",
                        "GET /patients/a1/firstName.json", "GET /patients/a1/medications.json"),
                database.requests.stream().map(r -> r.replaceFirst("\\?.*", "")).toList());
    }

    @Test
    void readingEveryFieldFetchesTheWholePatient() {
        repository.save(patient("a1", "John", Map.of("jansen", true)));
        database.requests.clear();

        repository.findAllForUser("jansen", EnumSet.allOf(PatientField.class));

        assertTrue(database.requests.stream().anyMatch(r -> r.startsWith("GET /patients/a1.json")));
    }

    @Test
    void staleIndexEntriesAreIgnored() {
        repository.save(patient("a1", "John", Map.of("jansen", true)));
        database.set("userPatients/devries/a1", new JsonPrimitive(true));
        database.set("userPatients/devries/gone", new JsonPrimitive(true));

        assertEquals(List.of(), repository.findAllForUser("devries"));
    }

    @Test
    void findAllReturnsEveryPatient() {
        repository.save(patient("a1", "John", Map.of("jansen", true)));
        repository.save(patient("b2", "Jane", Map.of("hendriks", true)));

        assertEquals(2, repository.findAll().size());
    }

    @Test
    void emptyDatabaseHasNoPatients() {
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void missingCareTeamAndMedicationsLoadEmpty() {
        database.set("patients/a1", JsonParser.parseString(
                "{\"firstName\":\"John\",\"lastName\":\"Doe\",\"weight\":70,\"height\":1.8}"));

        Patient loaded = repository.findById("a1").orElseThrow();

        assertTrue(loaded.careTeam().isEmpty());
        assertTrue(loaded.medications().isEmpty());
        assertEquals(70.0, loaded.weight());
    }

    @Test
    void savingWithoutAnIdIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> repository.save(patient(null, "John", Map.of("jansen", true))));
    }
}
