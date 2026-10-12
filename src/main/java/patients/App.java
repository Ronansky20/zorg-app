package patients;

import patients.repository.FirebaseClient;
import patients.repository.FirebasePatientRepository;
import patients.repository.FirebaseUserRepository;
import patients.repository.JsonPatientRepository;
import patients.repository.JsonUserRepository;
import patients.repository.PatientRepository;
import patients.repository.UserRepository;
import patients.service.PatientService;
import patients.service.UserService;
import patients.ui.ConsoleUI;

import java.net.URI;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {
        PatientRepository repository;
        UserRepository userRepository;

        String firebaseUrl = System.getenv("ZORG_FIREBASE_URL");
        if (firebaseUrl != null && !firebaseUrl.isBlank()) {
            FirebaseClient client = new FirebaseClient(URI.create(firebaseUrl),
                    () -> System.getenv("ZORG_FIREBASE_TOKEN"));
            repository = new FirebasePatientRepository(client);
            userRepository = new FirebaseUserRepository(client);
            System.out.println("Using Firebase at " + firebaseUrl);
        } else {
            repository = new JsonPatientRepository(Paths.get("patients.json"));
            userRepository = new JsonUserRepository(Paths.get("users.json"));
        }

        PatientService service = new PatientService(repository);
        UserService userService = new UserService(userRepository);
        ConsoleUI ui = new ConsoleUI(service, userService);
        ui.run();
    }
}
