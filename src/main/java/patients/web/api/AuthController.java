package patients.web.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import patients.web.session.LoginRequest;
import patients.web.session.SignIn;
import patients.web.session.Workspace;

import java.util.List;

@RestController
@RequestMapping("/api")
class AuthController {

    private final SignIn signIn;

    AuthController(SignIn signIn) {
        this.signIn = signIn;
    }

    @GetMapping("/auth/accounts")
    List<UserResponse> accounts() {
        return signIn.accounts().stream().map(UserResponse::of).toList();
    }

    @PostMapping("/auth/login")
    MeResponse login(@RequestBody LoginRequest login, HttpServletRequest request) {
        Workspace workspace = signIn.signIn(login);
        HttpSession session = request.getSession(true);
        request.changeSessionId();
        session.setAttribute(Workspace.SESSION_ATTRIBUTE, workspace);
        return MeResponse.of(workspace.user());
    }

    @PostMapping("/auth/logout")
    ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    MeResponse me(Workspace workspace) {
        return MeResponse.of(workspace.user());
    }
}
