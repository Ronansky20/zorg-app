package patients.repository;

import java.security.SecureRandom;
import java.util.Base64;

public final class Ids {

    private static final SecureRandom RANDOM = new SecureRandom();

    private Ids() {}

    public static String newId() {
        byte[] bytes = new byte[9];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
