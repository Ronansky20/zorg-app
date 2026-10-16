package patients.web.session;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class LoginThrottle {

    public static final Duration WINDOW = Duration.ofMinutes(15);
    public static final int MAX_PER_ACCOUNT = 5;
    public static final int MAX_PER_ADDRESS = 50;
    private static final int MAX_TRACKED = 10_000;

    private record Attempts(Instant until, int count) {}

    private final Clock clock;
    private final Map<String, Attempts> accounts = new ConcurrentHashMap<>();
    private final Map<String, Attempts> addresses = new ConcurrentHashMap<>();

    public LoginThrottle() {
        this(Clock.systemUTC());
    }

    LoginThrottle(Clock clock) {
        this.clock = clock;
    }

    public Workspace signIn(String account, String address, Supplier<Workspace> signIn) {
        acquire(addresses, address, MAX_PER_ADDRESS);
        if (account != null) {
            try {
                acquire(accounts, account, MAX_PER_ACCOUNT);
            } catch (TooManyAttemptsException e) {
                release(addresses, address);
                throw e;
            }
        }

        Workspace workspace;
        try {
            workspace = signIn.get();
        } catch (SignInFailedException e) {
            throw e;
        } catch (RuntimeException e) {
            release(addresses, address);
            if (account != null) {
                release(accounts, account);
            }
            throw e;
        }
        release(addresses, address);
        if (account != null) {
            accounts.remove(account);
        }
        return workspace;
    }

    private void acquire(Map<String, Attempts> attempts, String key, int limit) {
        Instant now = clock.instant();
        if (attempts.size() > MAX_TRACKED) {
            attempts.values().removeIf(a -> !now.isBefore(a.until()));
        }
        Attempts updated = attempts.compute(key, (k, current) -> current == null || !now.isBefore(current.until())
                ? new Attempts(now.plus(WINDOW), 1)
                : new Attempts(current.until(), Math.min(current.count() + 1, limit + 1)));
        if (updated.count() > limit) {
            throw new TooManyAttemptsException(Duration.between(now, updated.until()));
        }
    }

    private static void release(Map<String, Attempts> attempts, String key) {
        attempts.computeIfPresent(key, (k, current) ->
                current.count() <= 1 ? null : new Attempts(current.until(), current.count() - 1));
    }
}
