package patients.web.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import patients.model.Medication;
import patients.model.Patient;
import patients.model.PatientField;
import patients.model.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PatientResponse(String id, String firstName, String lastName, LocalDate birthDate, Double weight,
                              Double height, String address, List<Medication> medications,
                              List<CareTeamMember> careTeam) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CareTeamMember(String id, String name, String profession) {}

    static PatientResponse of(Patient patient, Set<PatientField> viewable, List<User> users) {
        return new PatientResponse(
                patient.id(),
                viewable.contains(PatientField.FIRST_NAME) ? patient.firstName() : null,
                viewable.contains(PatientField.LAST_NAME) ? patient.lastName() : null,
                viewable.contains(PatientField.DATE_OF_BIRTH) ? patient.birthDate() : null,
                viewable.contains(PatientField.WEIGHT) ? patient.weight() : null,
                viewable.contains(PatientField.HEIGHT) ? patient.height() : null,
                viewable.contains(PatientField.ADDRESS) ? patient.address() : null,
                viewable.contains(PatientField.MEDICATION) ? patient.medications() : null,
                careTeam(patient, users));
    }

    private static List<CareTeamMember> careTeam(Patient patient, List<User> users) {
        Map<String, User> byId = users.stream().collect(Collectors.toMap(User::id, Function.identity()));
        return patient.careTeam().keySet().stream()
                .filter(patient::hasMember)
                .map(id -> {
                    User user = byId.get(id);
                    return user == null
                            ? new CareTeamMember(id, null, null)
                            : new CareTeamMember(id, user.name(), user.profession().name());
                })
                .toList();
    }
}
