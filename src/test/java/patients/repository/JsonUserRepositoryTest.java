package patients.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import patients.model.Profession;
import patients.model.User;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonUserRepositoryTest {

    @TempDir
    Path tempDir;

    private Path usersFile() throws IOException {
        Path file = tempDir.resolve("users.json");
        Files.writeString(file, """
                {
                  "users": {
                    "jansen": { "name": "Dr. Max Jansen", "profession": "DOCTOR" },
                    "devries": { "name": "Pieter de Vries", "profession": "PHARMACIST" }
                  }
                }
                """);
        return file;
    }

    @Test
    void findAllReturnsUsersInFileOrderWithTheKeyAsId() throws IOException {
        JsonUserRepository repository = new JsonUserRepository(usersFile());

        assertEquals(List.of(
                new User("jansen", "Dr. Max Jansen", Profession.DOCTOR),
                new User("devries", "Pieter de Vries", Profession.PHARMACIST)),
                repository.findAll());
    }

    @Test
    void findByIdReturnsTheMatchingUserOrEmpty() throws IOException {
        JsonUserRepository repository = new JsonUserRepository(usersFile());

        assertEquals(Optional.of(new User("devries", "Pieter de Vries", Profession.PHARMACIST)),
                repository.findById("devries"));
        assertEquals(Optional.empty(), repository.findById("nobody"));
    }

    @Test
    void findAllOnMissingFileReturnsEmptyList() {
        assertTrue(new JsonUserRepository(tempDir.resolve("missing.json")).findAll().isEmpty());
    }

    @Test
    void fileWithoutUsersSectionIsRejected() throws IOException {
        Path file = tempDir.resolve("users.json");
        Files.writeString(file, "{ \"patients\": {} }");

        assertThrows(UncheckedIOException.class, () -> new JsonUserRepository(file).findAll());
    }

    @Test
    void theTrackedUsersFileHasAtLeastOneUserPerProfession() {
        List<User> users = new JsonUserRepository(Path.of("users.json")).findAll();

        for (Profession profession : Profession.values()) {
            assertTrue(users.stream().anyMatch(u -> u.profession() == profession), profession.label);
        }
    }
}
