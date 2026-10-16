package patients.web.api;

import jakarta.servlet.http.HttpSession;
import patients.model.Action;
import patients.model.PatientField;
import patients.model.User;
import patients.service.FieldPermissions;
import patients.service.ProfessionPermissions;

import java.util.List;
import java.util.Set;

public record MeResponse(UserResponse user, List<String> actions, List<String> viewableFields,
                         List<String> editableFields, int sessionTimeoutSeconds) {

    static MeResponse of(User user, HttpSession session) {
        Set<Action> actions = ProfessionPermissions.getAllowedActions(user.profession());
        Set<PatientField> editable = actions.contains(Action.EDIT_PATIENT)
                ? FieldPermissions.getEditableFields(user.profession())
                : Set.of();
        return new MeResponse(
                UserResponse.of(user),
                actions.stream().map(Action::name).toList(),
                keys(FieldPermissions.getViewableFields(user.profession())),
                keys(editable),
                session.getMaxInactiveInterval());
    }

    private static List<String> keys(Set<PatientField> fields) {
        return fields.stream().map(field -> field.key).toList();
    }
}
