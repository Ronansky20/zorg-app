package patients;

import patients.auth.FirebaseAuth;
import patients.service.PatientService;
import patients.service.UserService;
import patients.ui.ConsoleUI;

import java.nio.file.Path;

public class App {
    public static void main(String[] args) {
        Backend backend = Backend.fromEnvironment(System::getenv);
        Backend.Repositories repositories;
        FirebaseAuth auth = null;

        if (backend.usesFirebase()) {
            if (!backend.canSignIn()) {
                System.out.println(Backend.SIGN_IN_SETTINGS_MISSING);
                return;
            }
            auth = backend.newAuth();
            repositories = backend.firebase(auth);
            System.out.println("Using Firebase at " + backend.firebaseUrl());
        } else {
            repositories = Backend.json(Path.of(""));
        }

        PatientService service = new PatientService(repositories.patients());
        UserService userService = new UserService(repositories.users());
        ConsoleUI ui = new ConsoleUI(service, userService, auth);
        ui.run();
    }
}
