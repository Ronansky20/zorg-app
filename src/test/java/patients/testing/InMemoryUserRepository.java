package patients.testing;

import patients.model.User;
import patients.repository.UserRepository;

import java.util.List;
import java.util.Optional;

public class InMemoryUserRepository implements UserRepository {

    private final List<User> users;

    public InMemoryUserRepository(List<User> users) {
        this.users = List.copyOf(users);
    }

    @Override
    public List<User> findAll() {
        return users;
    }

    @Override
    public Optional<User> findById(String id) {
        return users.stream().filter(u -> u.id().equals(id)).findFirst();
    }
}
