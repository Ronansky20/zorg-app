package patients.model;

import java.time.LocalDate;
import java.time.Period;

public record Patient(
        String firstName,
        String lastName,
        LocalDate birthDate,
        double weight,
        double height,
        String address,
        Specialists specialists) {

    public double bmi() {
        return weight / (height * height);
    }

    public int age() {
        return age(LocalDate.now());
    }

    public int age(LocalDate today) {
        return Period.between(birthDate, today).getYears();
    }

    public record Specialists(
            String doctor,
            String pharmacist,
            String physiotherapist,
            String dentist) {}
}
