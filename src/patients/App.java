package patients;

import patients.repository.JsonPatientRepository;
import patients.repository.PatientRepository;
import patients.service.PatientService;
import patients.ui.ConsoleUI;

import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {
        PatientRepository repository = new JsonPatientRepository(Paths.get("patients.json"));
        PatientService service = new PatientService(repository);
        ConsoleUI ui = new ConsoleUI(service);
        ui.run();
    }
}