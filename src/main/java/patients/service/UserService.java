package patients.service;

import patients.model.Profession;
import patients.model.User;
import patients.repository.UserRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class UserService {

    private static final Comparator<User> BY_PROFESSION_THEN_NAME = Comparator.comparing(User::profession)
            .thenComparing(User::name, String.CASE_INSENSITIVE_ORDER);

    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public List<User> getAll() {
        return repository.findAll().stream()
                .sorted(BY_PROFESSION_THEN_NAME)
                .toList();
    }

    public List<User> getByProfession(Profession profession) {
        return getAll().stream()
                .filter(u -> u.profession() == profession)
                .toList();
    }

    public Optional<User> findById(String id) {
        return repository.findById(id);
    }
}
