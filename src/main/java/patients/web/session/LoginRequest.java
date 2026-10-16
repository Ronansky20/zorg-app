package patients.web.session;

import java.util.Locale;

public record LoginRequest(String userId, String email, String password) {

    public String account() {
        String id = email == null || email.isBlank() ? userId : email;
        return id == null || id.isBlank() ? null : id.strip().toLowerCase(Locale.ROOT);
    }
}
