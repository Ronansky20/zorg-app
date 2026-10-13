package patients.web.api;

import patients.model.Medication;
import patients.model.Patient;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record NewPatientRequest(String firstName, String lastName, LocalDate birthDate, Double weight,
                                Double height, String address, List<String> careTeam,
                                List<Medication> medications) {

    Patient toPatient() {
        Map<String, Boolean> team = new LinkedHashMap<>();
        if (careTeam != null) {
            careTeam.forEach(id -> team.put(id, true));
        }
        return new Patient(null, firstName, lastName, birthDate,
                weight == null ? 0 : weight, height == null ? 0 : height,
                address, team, medications);
    }
}
