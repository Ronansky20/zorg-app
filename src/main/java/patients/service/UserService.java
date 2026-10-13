package patients.service;

import patients.model.Profession;
import patients.model.User;
import patients.repository.UserRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class UserService {

    private final UserRepository repository;
    private final List<User> users = new ArrayList<>();

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public void load() {
        users.clear();
        users.addAll(repository.findAll());
        users.sort(Comparator.comparing(User::profession)
                .thenComparing(User::name, String.CASE_INSENSITIVE_ORDER));
    }

    public List<User> getAll() {
        return List.copyOf(users);
    }

    public List<User> getByProfession(Profession profession) {
        return users.stream()
                .filter(u -> u.profession() == profession)
                .toList();
    }

    public Optional<User> findById(String id) {
        return users.stream()
                .filter(u -> u.id().equals(id))
                .findFirst();
    }
}
