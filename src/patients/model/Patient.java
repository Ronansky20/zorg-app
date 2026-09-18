package patients.model;

public record Patient(
        String firstName,
        String lastName,
        int age,
        double weight,
        double height,
        String address,
        Specialists specialists,
        Medication medication) {

    public double bmi() {
        return weight / (height * height);
    }

    public record Specialists(
            String doctor,
            String pharmacist,
            String physician,
            String dentist) {}

    public record Medication(
            String name,
            String dosage,
            String frequency,
            String duration) {}
}