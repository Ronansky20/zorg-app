package patients.model;

public enum PatientField {
    FIRST_NAME("First name", "firstName"),
    LAST_NAME("Last name", "lastName"),
    DATE_OF_BIRTH("Date of birth", "birthDate"),
    WEIGHT("Weight", "weight"),
    HEIGHT("Height", "height"),
    ADDRESS("Address", "address"),
    MEDICATION("Medication", "medications");

    public final String label;
    public final String key;

    private PatientField(String label, String key) {
        this.label = label;
        this.key = key;
    }

    public Object valueOf(Patient patient) {
        return switch (this) {
            case FIRST_NAME -> patient.firstName();
            case LAST_NAME -> patient.lastName();
            case DATE_OF_BIRTH -> patient.birthDate();
            case WEIGHT -> patient.weight();
            case HEIGHT -> patient.height();
            case ADDRESS -> patient.address();
            case MEDICATION -> patient.medications();
        };
    }
}
