package patients.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import patients.model.Profession;
import patients.model.User;
import patients.testing.InMemoryUserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserServiceTest {

    private static final User JANSEN = new User("jansen", "Dr. Max Jansen", Profession.DOCTOR);
    private static final User HENDRIKS = new User("hendriks", "Dr. Noor Hendriks", Profession.DOCTOR);
    private static final User DE_VRIES = new User("devries", "Pieter de Vries", Profession.PHARMACIST);

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(new InMemoryUserRepository(List.of(JANSEN, DE_VRIES, HENDRIKS)));
    }

    @Test
    void getAllIsSortedByProfessionThenName() {
        assertEquals(List.of(JANSEN, HENDRIKS, DE_VRIES), service.getAll());
    }

    @Test
    void getByProfessionKeepsOnlyThatProfession() {
        assertEquals(List.of(JANSEN, HENDRIKS), service.getByProfession(Profession.DOCTOR));
        assertEquals(List.of(), service.getByProfession(Profession.DENTIST));
    }

    @Test
    void findByIdLooksUpUsers() {
        assertEquals(Optional.of(DE_VRIES), service.findById("devries"));
        assertEquals(Optional.empty(), service.findById("nobody"));
    }
}
