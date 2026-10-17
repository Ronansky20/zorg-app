package patients.web.api;

import java.util.List;

public record ConfigResponse(String mode, List<Option> professions, List<Option> fields, List<Option> actions,
                             List<Option> medicationTypes, List<MedicationOption> medications) {

    public record Option(String id, String label) {}

    public record MedicationOption(String name, String type) {}
}
