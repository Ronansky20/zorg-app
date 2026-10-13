package patients.web.api;

import patients.model.User;

public record UserResponse(String id, String name, String profession) {

    static UserResponse of(User user) {
        return new UserResponse(user.id(), user.name(), user.profession().name());
    }
}
