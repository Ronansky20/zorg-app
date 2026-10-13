package patients.web.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import patients.web.session.Workspace;

import java.util.List;

@RestController
class UserController {

    @GetMapping("/api/users")
    List<UserResponse> users(Workspace workspace) {
        return workspace.users().getAll().stream().map(UserResponse::of).toList();
    }
}
