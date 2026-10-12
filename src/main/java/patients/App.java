package patients;

import patients.repository.JsonPatientRepository;
import patients.repository.JsonUserRepository;
import patients.repository.PatientRepository;
import patients.repository.UserRepository;
import patients.service.PatientService;
import patients.service.UserService;
import patients.ui.ConsoleUI;

import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {
        PatientRepository repository = new JsonPatientRepository(Paths.get("patients.json"));
        UserRepository userRepository = new JsonUserRepository(Paths.get("users.json"));
        PatientService service = new PatientService(repository);
        UserService userService = new UserService(userRepository);
        ConsoleUI ui = new ConsoleUI(service, userService);
        ui.run();
    }
}
