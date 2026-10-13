package patients.web.session;

import patients.Backend;
import patients.model.User;
import patients.service.PatientService;
import patients.service.UserService;

import java.util.List;

public class LocalSignIn implements SignIn {

    private final PatientService patients;
    private final UserService users;

    public LocalSignIn(Backend.Repositories repositories) {
        this.patients = new PatientService(repositories.patients());
        this.users = new UserService(repositories.users());
    }

    @Override
    public boolean requiresPassword() {
        return false;
    }

    @Override
    public List<User> accounts() {
        return users.getAll();
    }

    @Override
    public Workspace signIn(LoginRequest request) {
        if (request.userId() == null || request.userId().isBlank()) {
            throw new SignInFailedException("Choose an account");
        }
        User user = users.findById(request.userId())
                .orElseThrow(() -> new SignInFailedException("Unknown account"));
        return new Workspace(user, patients, users);
    }
}
