package patients.repository;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import patients.model.User;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JsonUserRepository implements UserRepository {

    private static final String USERS_KEY = "users";
    private static final Type USERS_TYPE =
            new TypeToken<LinkedHashMap<String, User>>() {}.getType();

    private final Path file;
    private final Gson gson = GsonFactory.builder().create();

    public JsonUserRepository(Path file) {
        this.file = file;
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        for (Map.Entry<String, User> entry : readFile().entrySet()) {
            users.add(entry.getValue().withId(entry.getKey()));
        }
        return users;
    }

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(readFile().get(id)).map(u -> u.withId(id));
    }

    private Map<String, User> readFile() {
        return JsonFiles.readSection(gson, file, USERS_KEY, USERS_TYPE);
    }
}
