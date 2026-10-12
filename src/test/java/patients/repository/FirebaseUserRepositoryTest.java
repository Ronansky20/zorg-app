package patients.repository;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import patients.model.Profession;
import patients.model.User;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FirebaseUserRepositoryTest {

    private FakeRealtimeDatabase database;
    private FirebaseUserRepository repository;

    @BeforeEach
    void setUp() throws IOException {
        database = new FakeRealtimeDatabase();
        repository = new FirebaseUserRepository(new FirebaseClient(database.url(), () -> null));
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    @Test
    void findAllReturnsUsersWithTheKeyAsId() {
        database.set("users", JsonParser.parseString("""
                {
                  "jansen": { "name": "Dr. Max Jansen", "profession": "DOCTOR" },
                  "devries": { "name": "Pieter de Vries", "profession": "PHARMACIST" }
                }
                """));

        assertEquals(List.of(
                new User("jansen", "Dr. Max Jansen", Profession.DOCTOR),
                new User("devries", "Pieter de Vries", Profession.PHARMACIST)),
                repository.findAll());
    }

    @Test
    void findByIdReturnsTheUserOrEmpty() {
        database.set("users/jansen", JsonParser.parseString(
                "{ \"name\": \"Dr. Max Jansen\", \"profession\": \"DOCTOR\" }"));

        assertEquals(Optional.of(new User("jansen", "Dr. Max Jansen", Profession.DOCTOR)),
                repository.findById("jansen"));
        assertEquals(Optional.empty(), repository.findById("nobody"));
    }

    @Test
    void emptyDatabaseHasNoUsers() {
        assertTrue(repository.findAll().isEmpty());
    }
}
