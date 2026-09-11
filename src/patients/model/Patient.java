package patients.model;

public record Patient(
        String firstName,
        String lastName,
        int age,
        double weight,
        double height,
        String address) {

    public double bmi() {
        return weight / (height * height);
    }
}