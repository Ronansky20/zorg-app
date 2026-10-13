package patients.web.session;

public class NotSignedInException extends RuntimeException {

    public NotSignedInException() {
        super("Please sign in");
    }
}
