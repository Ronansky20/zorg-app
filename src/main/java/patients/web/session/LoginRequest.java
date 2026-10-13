package patients.web.session;

public record LoginRequest(String userId, String email, String password) {}
