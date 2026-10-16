package patients.web.session;

import org.junit.jupiter.api.Test;
import patients.testing.MutableClock;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginThrottleTest {

    private static final Workspace WORKSPACE = new Workspace(null, null, null);

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-16T10:00:00Z"));
    private final LoginThrottle throttle = new LoginThrottle(clock);

    @Test
    void anAccountIsLockedAfterTooManyFailures() {
        failRepeatedly("jansen", "10.0.0.1", LoginThrottle.MAX_PER_ACCOUNT);

        TooManyAttemptsException e = assertThrows(TooManyAttemptsException.class,
                () -> throttle.signIn("jansen", "10.0.0.2", () -> WORKSPACE));
        assertEquals(LoginThrottle.WINDOW, e.retryAfter());
        assertEquals("Too many failed attempts, try again in 15 minutes", e.getMessage());
    }

    @Test
    void theLockEndsAfterTheWindow() {
        failRepeatedly("jansen", "10.0.0.1", LoginThrottle.MAX_PER_ACCOUNT);

        clock.advance(LoginThrottle.WINDOW);

        assertSame(WORKSPACE, throttle.signIn("jansen", "10.0.0.1", () -> WORKSPACE));
    }

    @Test
    void successResetsTheAccount() {
        failRepeatedly("jansen", "10.0.0.1", LoginThrottle.MAX_PER_ACCOUNT - 1);
        throttle.signIn("jansen", "10.0.0.1", () -> WORKSPACE);

        failRepeatedly("jansen", "10.0.0.1", LoginThrottle.MAX_PER_ACCOUNT - 1);

        assertSame(WORKSPACE, throttle.signIn("jansen", "10.0.0.1", () -> WORKSPACE));
    }

    @Test
    void anAddressIsLockedAfterFailuresOnManyAccounts() {
        for (int i = 0; i < LoginThrottle.MAX_PER_ADDRESS; i++) {
            failRepeatedly("user" + i, "10.0.0.1", 1);
        }

        assertThrows(TooManyAttemptsException.class, () -> throttle.signIn("jansen", "10.0.0.1", () -> WORKSPACE));
        assertSame(WORKSPACE, throttle.signIn("jansen", "10.0.0.2", () -> WORKSPACE));
    }

    @Test
    void successfulLoginsDoNotCountForTheAddress() {
        for (int i = 0; i < LoginThrottle.MAX_PER_ADDRESS * 2; i++) {
            throttle.signIn("user" + i, "10.0.0.1", () -> WORKSPACE);
        }

        assertSame(WORKSPACE, throttle.signIn("jansen", "10.0.0.1", () -> WORKSPACE));
    }

    @Test
    void serviceErrorsDoNotCount() {
        for (int i = 0; i < LoginThrottle.MAX_PER_ACCOUNT * 2; i++) {
            assertThrows(UncheckedIOException.class, () -> throttle.signIn("jansen", "10.0.0.1", () -> {
                throw new UncheckedIOException(new IOException("Firebase is down"));
            }));
        }

        assertSame(WORKSPACE, throttle.signIn("jansen", "10.0.0.1", () -> WORKSPACE));
    }

    @Test
    void requestsWithoutAnAccountOnlyCountForTheAddress() {
        failRepeatedly(null, "10.0.0.1", LoginThrottle.MAX_PER_ADDRESS);

        assertThrows(TooManyAttemptsException.class, () -> throttle.signIn("jansen", "10.0.0.1", () -> WORKSPACE));
    }

    @Test
    void retryAfterCountsDownFromTheFirstFailure() {
        failRepeatedly("jansen", "10.0.0.1", LoginThrottle.MAX_PER_ACCOUNT);
        clock.advance(Duration.ofMinutes(14).plusSeconds(30));

        TooManyAttemptsException e = assertThrows(TooManyAttemptsException.class,
                () -> throttle.signIn("jansen", "10.0.0.1", () -> WORKSPACE));
        assertEquals(Duration.ofSeconds(30), e.retryAfter());
        assertEquals("Too many failed attempts, try again in 1 minute", e.getMessage());
    }

    private void failRepeatedly(String account, String address, int times) {
        for (int i = 0; i < times; i++) {
            assertThrows(SignInFailedException.class, () -> throttle.signIn(account, address, () -> {
                throw new SignInFailedException("Wrong email or password");
            }));
        }
    }
}
