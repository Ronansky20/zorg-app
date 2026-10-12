package patients.repository;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import patients.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FirebaseUserRepository implements UserRepository {

    private final FirebaseClient client;
    private final Gson gson = GsonFactory.builder().create();

    public FirebaseUserRepository(FirebaseClient client) {
        this.client = client;
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        JsonElement tree = client.get("users");
        if (tree.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : tree.getAsJsonObject().entrySet()) {
                users.add(gson.fromJson(entry.getValue(), User.class).withId(entry.getKey()));
            }
        }
        return users;
    }

    @Override
    public Optional<User> findById(String id) {
        JsonElement element = client.get("users/" + id);
        return element.isJsonObject()
                ? Optional.of(gson.fromJson(element, User.class).withId(id))
                : Optional.empty();
    }
}
