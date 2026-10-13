package patients.repository;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import patients.model.Medication;
import patients.model.Patient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonPatientRepositoryTest {

    @TempDir
    Path tempDir;

    private static Patient patient(String id, String firstName) {
        return new Patient(id, firstName, "Doe", LocalDate.of(1995, 6, 15), 70.0, 1.8, "Elm Street 58",
                new Patient.Specialists("Doctor Anon", "Pharmacist Anon", "Physio Anon", "Dentist Anon"),
                List.of(new Medication("Paracetamol", "500 mg", "3x daily")));
    }

    @Test
    void findAllOnMissingFileReturnsEmptyList() {
        JsonPatientRepository repository = new JsonPatientRepository(tempDir.resolve("missing.json"));

        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void savedPatientsCanBeLoadedBack() {
        Path file = tempDir.resolve("patients.json");
        JsonPatientRepository repository = new JsonPatientRepository(file);

        repository.save(patient("a1", "John"));
        repository.save(patient("b2", "Jane"));

        assertEquals(List.of(patient("a1", "John"), patient("b2", "Jane")), repository.findAll());
    }

    @Test
    void savedPatientSurvivesANewRepositoryInstance() {
        Path file = tempDir.resolve("patients.json");
        new JsonPatientRepository(file).save(patient("a1", "John"));

        assertEquals(List.of(patient("a1", "John")), new JsonPatientRepository(file).findAll());
    }

    @Test
    void findByIdReturnsTheMatchingPatientOrEmpty() {
        JsonPatientRepository repository = new JsonPatientRepository(tempDir.resolve("patients.json"));
        repository.save(patient("a1", "John"));

        assertEquals(Optional.of(patient("a1", "John")), repository.findById("a1"));
        assertEquals(Optional.empty(), repository.findById("nope"));
    }

    @Test
    void savingAnExistingIdReplacesTheRecord() {
        JsonPatientRepository repository = new JsonPatientRepository(tempDir.resolve("patients.json"));
        repository.save(patient("a1", "John"));

        repository.save(patient("a1", "Johnny"));

        assertEquals(List.of(patient("a1", "Johnny")), repository.findAll());
    }

    @Test
    void savingWithoutAnIdIsRejected() {
        JsonPatientRepository repository = new JsonPatientRepository(tempDir.resolve("patients.json"));

        assertThrows(IllegalArgumentException.class, () -> repository.save(patient(null, "John")));
    }

    @Test
    void fileIsKeyedByIdWithoutRepeatingTheId() throws IOException {
        Path file = tempDir.resolve("patients.json");
        new JsonPatientRepository(file).save(patient("a1", "John"));

        JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        JsonObject record = root.getAsJsonObject("patients").getAsJsonObject("a1");
        assertEquals("John", record.get("firstName").getAsString());
        assertFalse(record.has("id"));
    }

    @Test
    void birthDateIsStoredAsIsoString() throws IOException {
        Path file = tempDir.resolve("patients.json");
        new JsonPatientRepository(file).save(patient("a1", "John"));

        assertTrue(Files.readString(file).contains("\"birthDate\": \"1995-06-15\""));
    }

    @Test
    void medicationsAreStoredAndLoadedBack() throws IOException {
        Path file = tempDir.resolve("patients.json");
        new JsonPatientRepository(file).save(patient("a1", "John"));

        assertTrue(Files.readString(file).contains("\"name\": \"Paracetamol\""));
        assertEquals(List.of(new Medication("Paracetamol", "500 mg", "3x daily")),
                new JsonPatientRepository(file).findAll().get(0).medications());
    }

    @Test
    void recordWithoutMedicationsLoadsWithAnEmptyList() throws IOException {
        Path file = tempDir.resolve("patients.json");
        Files.writeString(file, """
                {
                  "patients": {
                    "a1": {
                      "firstName": "John",
                      "lastName": "Doe",
                      "birthDate": "1995-06-15",
                      "weight": 70.0,
                      "height": 1.8,
                      "address": "Elm Street 58"
                    }
                  }
                }
                """);

        List<Patient> loaded = new JsonPatientRepository(file).findAll();

        assertEquals("a1", loaded.get(0).id());
        assertTrue(loaded.get(0).medications().isEmpty());
    }

    @Test
    void oldFormatIsRejectedAndNeverOverwritten() throws IOException {
        Path file = tempDir.resolve("patients.json");
        String oldFormat = """
                {
                  "abc": [
                    { "firstName": "John", "lastName": "Doe", "weight": 70.0, "height": 1.8 }
                  ]
                }
                """;
        Files.writeString(file, oldFormat);
        JsonPatientRepository repository = new JsonPatientRepository(file);

        assertThrows(UncheckedIOException.class, repository::findAll);
        assertThrows(UncheckedIOException.class, () -> repository.save(patient("a1", "Jane")));
        assertEquals(oldFormat, Files.readString(file));
    }

    @Test
    void findAllOnEmptyObjectReturnsEmptyList() throws IOException {
        Path file = tempDir.resolve("empty.json");
        Files.writeString(file, "{}");

        assertTrue(new JsonPatientRepository(file).findAll().isEmpty());
    }

    @Test
    void findAllOnEmptyFileReturnsEmptyList() throws IOException {
        Path file = tempDir.resolve("empty.json");
        Files.writeString(file, "");

        assertTrue(new JsonPatientRepository(file).findAll().isEmpty());
    }

    @Test
    void findAllOnMalformedJsonThrowsJsonSyntaxException() throws IOException {
        Path file = tempDir.resolve("broken.json");
        Files.writeString(file, "{ this is not valid json");
        JsonPatientRepository repository = new JsonPatientRepository(file);

        assertThrows(JsonSyntaxException.class, repository::findAll);
    }
}
