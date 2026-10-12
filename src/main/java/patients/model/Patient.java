package patients.model;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

public record Patient(
        String firstName,
        String lastName,
        LocalDate birthDate,
        double weight,
        double height,
        String address,
        Specialists specialists,
        List<Medication> medications) {

    public Patient {
        medications = medications == null ? List.of() : List.copyOf(medications);
    }

    public Patient withFirstName(String newFirstName) {
        return new Patient(
                newFirstName, this.lastName(),
                this.birthDate(),
                this.weight(), this.height(),
                this.address(),
                this.specialists(),
                this.medications());
    }

    public Patient withLastName(String newLastName) {
        return new Patient(
                this.firstName(), newLastName,
                this.birthDate(),
                this.weight(), this.height(),
                this.address(),
                this.specialists(),
                this.medications());
    }

    public Patient withBirthDate(LocalDate newBirthDate) {
        return new Patient(
                this.firstName(), this.lastName(),
                newBirthDate,
                this.weight(), this.height(),
                this.address(),
                this.specialists(),
                this.medications());
    }

    public Patient withWeight(double newWeight) {
        return new Patient(
                this.firstName(), this.lastName(),
                this.birthDate(),
                newWeight, this.height(),
                this.address(),
                this.specialists(),
                this.medications());
    }

    public Patient withHeight(double newHeight) {
        return new Patient(
                this.firstName(), this.lastName(),
                this.birthDate(),
                this.weight(), newHeight,
                this.address(),
                this.specialists(),
                this.medications());
    }

    public Patient withAddress(String newAddress) {
        return new Patient(
                this.firstName(), this.lastName(),
                this.birthDate(),
                this.weight(), this.height(),
                newAddress,
                this.specialists(),
                this.medications());
    }

    public Patient withMedications(List<Medication> newMedications) {
        return new Patient(
                this.firstName(), this.lastName(),
                this.birthDate(),
                this.weight(), this.height(),
                this.address(),
                this.specialists(),
                newMedications);
    }

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
            String dentist) {
    }
}
