package patients.model;

public enum PatientField {
    FIRST_NAME("First name"),
    LAST_NAME("Last name"),
    DATE_OF_BIRTH("Date of birth"),
    WEIGHT("Weight"),
    HEIGHT("Height"),
    ADDRESS("Address");

    public final String label;

    private PatientField(String label) {
        this.label = label;
    }
}
