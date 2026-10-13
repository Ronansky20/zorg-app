package patients.web.session;

import patients.model.User;

import java.util.List;

public interface SignIn {

    boolean requiresPassword();

    List<User> accounts();

    Workspace signIn(LoginRequest request);
}
