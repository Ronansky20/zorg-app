package patients.auth;

public interface Authenticator {
    String signIn(String email, String password);
}
