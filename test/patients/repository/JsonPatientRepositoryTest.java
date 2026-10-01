package patients.repository;

import com.google.gson.JsonSyntaxException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import patients.model.Patient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonPatientRepositoryTest {

    @TempDir
    Path tempDir;

    private static Patient patient(String firstName) {
        return new Patient(firstName, "Doe", 30, 70.0, 1.8, "Elm Street 58",
                new Patient.Specialists("Doctor Anon", "Pharmacist Anon", "Physio Anon", "Dentist Anon"));
    }

    @Test
    void loadAllOnMissingFileReturnsEmptyList() {
        JsonPatientRepository repository = new JsonPatientRepository(tempDir.resolve("missing.json"));

        assertTrue(repository.loadAll().isEmpty());
    }

    @Test
    void savedPatientsCanBeLoadedBack() {
        Path file = tempDir.resolve("patients.json");
        JsonPatientRepository repository = new JsonPatientRepository(file);
        List<Patient> original = List.of(patient("John"), patient("Jane"));

        repository.saveAll(original);
        List<Patient> loaded = repository.loadAll();

        assertEquals(original, loaded);
    }

    @Test
    void savingTwiceReusesTheSameGroupKey() throws IOException {
        Path file = tempDir.resolve("patients.json");
        JsonPatientRepository repository = new JsonPatientRepository(file);

        repository.saveAll(List.of(patient("John")));
        String firstKeyLine = firstKeyOf(file);

        repository.saveAll(List.of(patient("John"), patient("Jane")));
        String secondKeyLine = firstKeyOf(file);

        assertEquals(firstKeyLine, secondKeyLine);
    }

    @Test
    void loadingPreservesTheGroupKeyFromDiskAcrossRepositoryInstances() throws IOException {
        Path file = tempDir.resolve("patients.json");
        new JsonPatientRepository(file).saveAll(List.of(patient("John")));
        String keyBefore = firstKeyOf(file);

        JsonPatientRepository second = new JsonPatientRepository(file);
        second.loadAll();
        second.saveAll(List.of(patient("Jane")));

        assertEquals(keyBefore, firstKeyOf(file));
    }

    @Test
    void loadAllOnEmptyFileReturnsEmptyList() throws IOException {
        Path file = tempDir.resolve("empty.json");
        Files.writeString(file, "{}");
        JsonPatientRepository repository = new JsonPatientRepository(file);

        assertTrue(repository.loadAll().isEmpty());
    }

    @Test
    void loadAllOnMalformedJsonThrowsJsonSyntaxException() throws IOException {
        Path file = tempDir.resolve("broken.json");
        Files.writeString(file, "{ this is not valid json");
        JsonPatientRepository repository = new JsonPatientRepository(file);

        assertThrows(JsonSyntaxException.class, repository::loadAll);
    }

    private static String firstKeyOf(Path file) throws IOException {
        String content = Files.readString(file);
        int firstQuote = content.indexOf('"');
        int secondQuote = content.indexOf('"', firstQuote + 1);
        return content.substring(firstQuote, secondQuote + 1);
    }
}
