package patients.model;

public enum MedicationType {
    PILL("Pill"),
    INJECTION("Injection"),
    OINTMENT("Ointment");

    public final String label;

    private MedicationType(String label) {
        this.label = label;
    }
}
