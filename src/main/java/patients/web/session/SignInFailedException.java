package patients.web.session;

public class SignInFailedException extends RuntimeException {

    public SignInFailedException(String message) {
        super(message);
    }
}
