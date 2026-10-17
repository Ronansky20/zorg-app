package patients.web.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import patients.model.Action;
import patients.model.MedicationCatalog;
import patients.model.MedicationType;
import patients.model.PatientField;
import patients.model.Profession;
import patients.web.api.ConfigResponse.MedicationOption;
import patients.web.api.ConfigResponse.Option;
import patients.web.session.SignIn;

import java.util.Arrays;

@RestController
class ConfigController {

    private final SignIn signIn;

    ConfigController(SignIn signIn) {
        this.signIn = signIn;
    }

    @GetMapping("/api/config")
    ConfigResponse config() {
        return new ConfigResponse(
                signIn.requiresPassword() ? "firebase" : "json",
                Arrays.stream(Profession.values()).map(p -> new Option(p.name(), p.label)).toList(),
                Arrays.stream(PatientField.values()).map(f -> new Option(f.key, f.label)).toList(),
                Arrays.stream(Action.values()).map(a -> new Option(a.name(), a.label)).toList(),
                Arrays.stream(MedicationType.values()).map(t -> new Option(t.name(), t.label)).toList(),
                Arrays.stream(MedicationCatalog.values())
                        .map(m -> new MedicationOption(m.label, m.type.name())).toList());
    }
}
