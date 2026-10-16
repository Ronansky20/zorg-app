package patients.web.session;

import patients.Backend;
import patients.auth.FirebaseAuth;
import patients.model.User;
import patients.service.PatientService;
import patients.service.UserService;

import java.io.UncheckedIOException;
import java.util.List;

public class FirebaseSignIn implements SignIn {

    private final Backend backend;

    public FirebaseSignIn(Backend backend) {
        this.backend = backend;
    }

    @Override
    public boolean requiresPassword() {
        return true;
    }

    @Override
    public List<User> accounts() {
        return List.of();
    }

    @Override
    public Workspace signIn(LoginRequest request) {
        if (isBlank(request.email()) || isBlank(request.password())) {
            throw new SignInFailedException("Enter your email and password");
        }
        FirebaseAuth auth = backend.newAuth();
        String uid;
        try {
            uid = auth.signIn(request.email(), request.password());
        } catch (SecurityException e) {
            throw new SignInFailedException(e.getMessage());
        } catch (UncheckedIOException e) {
            throw new UncheckedIOException("Can't reach the sign-in service, try again later", e.getCause());
        }

        Backend.Repositories repositories = backend.firebase(auth);
        UserService users = new UserService(repositories.users());
        User user = users.findById(uid).orElseThrow(() -> new SecurityException(
                "Your login has no account in the database, ask an administrator to add it"));
        return new Workspace(user, new PatientService(repositories.patients()), users);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
