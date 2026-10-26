package patients.web.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import patients.model.Profession;
import patients.model.User;
import patients.web.session.Workspace;

import java.util.List;

@RestController
class UserController {

    @GetMapping("/api/users")
    List<UserResponse> users(Workspace workspace, @RequestParam(required = false) Profession profession) {
        List<User> users = profession == null
                ? workspace.users().getAll()
                : workspace.users().getByProfession(profession);
        return users.stream().map(UserResponse::of).toList();
    }
}
