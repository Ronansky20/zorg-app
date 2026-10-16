package patients.web.session;

import java.time.Duration;

public class TooManyAttemptsException extends RuntimeException {

    private final Duration retryAfter;

    public TooManyAttemptsException(Duration retryAfter) {
        super("Too many failed attempts, try again in " + minutes(retryAfter));
        this.retryAfter = retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }

    private static String minutes(Duration duration) {
        long minutes = Math.max(1, (duration.toSeconds() + 59) / 60);
        return minutes == 1 ? "1 minute" : minutes + " minutes";
    }
}
