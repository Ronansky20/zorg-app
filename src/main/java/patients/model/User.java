package patients.model;

public record User(String id, String name, Profession profession) {

    public User withId(String newId) {
        return new User(newId, name, profession);
    }
}
