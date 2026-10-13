package patients.web.api;

import patients.model.Medication;
import patients.model.Patient;

import java.time.LocalDate;
import java.util.List;

public record PatientChanges(String firstName, String lastName, LocalDate birthDate, Double weight,
                             Double height, String address, List<Medication> medications) {

    Patient applyTo(Patient patient) {
        Patient result = patient;
        if (firstName != null) {
            result = result.withFirstName(firstName);
        }
        if (lastName != null) {
            result = result.withLastName(lastName);
        }
        if (birthDate != null) {
            result = result.withBirthDate(birthDate);
        }
        if (weight != null) {
            result = result.withWeight(weight);
        }
        if (height != null) {
            result = result.withHeight(height);
        }
        if (address != null) {
            result = result.withAddress(address);
        }
        if (medications != null) {
            result = result.withMedications(medications);
        }
        return result;
    }
}
