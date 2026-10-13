package patients.model;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record Patient(
        String id,
        String firstName,
        String lastName,
        LocalDate birthDate,
        double weight,
        double height,
        String address,
        Map<String, Boolean> careTeam,
        List<Medication> medications) {

    public Patient {
        careTeam = careTeam == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(careTeam));
        medications = medications == null ? List.of() : List.copyOf(medications);
    }

    public boolean hasMember(String userId) {
        return Boolean.TRUE.equals(careTeam.get(userId));
    }

    public Patient withId(String newId) {
        return new Patient(
                newId,
                this.firstName(), this.lastName(),
                this.birthDate(),
                this.weight(), this.height(),
                this.address(),
                this.careTeam(),
                this.medications());
    }

    public Patient withFirstName(String newFirstName) {
        return new Patient(
                this.id(),
                newFirstName, this.lastName(),
                this.birthDate(),
                this.weight(), this.height(),
                this.address(),
                this.careTeam(),
                this.medications());
    }

    public Patient withLastName(String newLastName) {
        return new Patient(
                this.id(),
                this.firstName(), newLastName,
                this.birthDate(),
                this.weight(), this.height(),
                this.address(),
                this.careTeam(),
                this.medications());
    }

    public Patient withBirthDate(LocalDate newBirthDate) {
        return new Patient(
                this.id(),
                this.firstName(), this.lastName(),
                newBirthDate,
                this.weight(), this.height(),
                this.address(),
                this.careTeam(),
                this.medications());
    }

    public Patient withWeight(double newWeight) {
        return new Patient(
                this.id(),
                this.firstName(), this.lastName(),
                this.birthDate(),
                newWeight, this.height(),
                this.address(),
                this.careTeam(),
                this.medications());
    }

    public Patient withHeight(double newHeight) {
        return new Patient(
                this.id(),
                this.firstName(), this.lastName(),
                this.birthDate(),
                this.weight(), newHeight,
                this.address(),
                this.careTeam(),
                this.medications());
    }

    public Patient withAddress(String newAddress) {
        return new Patient(
                this.id(),
                this.firstName(), this.lastName(),
                this.birthDate(),
                this.weight(), this.height(),
                newAddress,
                this.careTeam(),
                this.medications());
    }

    public Patient withCareTeam(Map<String, Boolean> newCareTeam) {
        return new Patient(
                this.id(),
                this.firstName(), this.lastName(),
                this.birthDate(),
                this.weight(), this.height(),
                this.address(),
                newCareTeam,
                this.medications());
    }

    public Patient withMedications(List<Medication> newMedications) {
        return new Patient(
                this.id(),
                this.firstName(), this.lastName(),
                this.birthDate(),
                this.weight(), this.height(),
                this.address(),
                this.careTeam(),
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
}
