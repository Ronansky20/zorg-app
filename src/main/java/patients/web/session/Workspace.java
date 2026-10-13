package patients.web.session;

import patients.model.User;
import patients.service.PatientService;
import patients.service.UserService;

public record Workspace(User user, PatientService patients, UserService users) {

    public static final String SESSION_ATTRIBUTE = "zorg.workspace";
}
