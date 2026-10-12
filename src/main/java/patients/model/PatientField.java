package patients.model;

public enum PatientField {
    FIRST_NAME("First name"),
    LAST_NAME("Last name"),
    DATE_OF_BIRTH("Date of birth"),
    WEIGHT("Weight"),
    HEIGHT("Height"),
    ADDRESS("Address"),
    MEDICATION("Medication");

    public final String label;

    private PatientField(String label) {
        this.label = label;
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
