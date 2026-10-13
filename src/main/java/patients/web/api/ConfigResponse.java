package patients.web.api;

import java.util.List;

public record ConfigResponse(String mode, List<Option> professions, List<Option> fields, List<Option> actions) {

    public record Option(String id, String label) {}
}
