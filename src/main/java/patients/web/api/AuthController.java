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
import patients.web.session.LoginThrottle;
import patients.web.session.SignIn;
import patients.web.session.Workspace;

import java.util.List;

@RestController
@RequestMapping("/api")
class AuthController {

    private final SignIn signIn;
    private final LoginThrottle throttle;

    AuthController(SignIn signIn, LoginThrottle throttle) {
        this.signIn = signIn;
        this.throttle = throttle;
    }

    @GetMapping("/auth/accounts")
    List<UserResponse> accounts() {
        return signIn.accounts().stream().map(UserResponse::of).toList();
    }

    @PostMapping("/auth/login")
    MeResponse login(@RequestBody LoginRequest login, HttpServletRequest request) {
        Workspace workspace = throttle.signIn(login.account(), request.getRemoteAddr(), () -> signIn.signIn(login));
        HttpSession session = request.getSession(true);
        request.changeSessionId();
        session.setAttribute(Workspace.SESSION_ATTRIBUTE, workspace);
        return MeResponse.of(workspace.user(), session);
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
    MeResponse me(Workspace workspace, HttpServletRequest request) {
        return MeResponse.of(workspace.user(), request.getSession(false));
    }
}
