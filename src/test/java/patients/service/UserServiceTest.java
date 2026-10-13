package patients.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import patients.model.Profession;
import patients.model.User;
import patients.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceTest {

    private static final User JANSEN = new User("jansen", "Dr. Max Jansen", Profession.DOCTOR);
    private static final User HENDRIKS = new User("hendriks", "Dr. Noor Hendriks", Profession.DOCTOR);
    private static final User DE_VRIES = new User("devries", "Pieter de Vries", Profession.PHARMACIST);

    private UserService service;

    @BeforeEach
    void setUp() {
        List<User> users = List.of(JANSEN, DE_VRIES, HENDRIKS);
        service = new UserService(new UserRepository() {
            @Override
            public List<User> findAll() {
                return users;
            }

            @Override
            public Optional<User> findById(String id) {
                return users.stream().filter(u -> u.id().equals(id)).findFirst();
            }
        });
    }

    @Test
    void getAllIsEmptyBeforeLoading() {
        assertTrue(service.getAll().isEmpty());
    }

    @Test
    void getAllIsSortedByProfessionThenName() {
        service.load();

        assertEquals(List.of(JANSEN, HENDRIKS, DE_VRIES), service.getAll());
    }

    @Test
    void getByProfessionKeepsOnlyThatProfession() {
        service.load();

        assertEquals(List.of(JANSEN, HENDRIKS), service.getByProfession(Profession.DOCTOR));
        assertEquals(List.of(), service.getByProfession(Profession.DENTIST));
    }

    @Test
    void findByIdLooksUpLoadedUsers() {
        service.load();

        assertEquals(Optional.of(DE_VRIES), service.findById("devries"));
        assertEquals(Optional.empty(), service.findById("nobody"));
    }
}
