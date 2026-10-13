package patients.service;

public class PatientNotFoundException extends IllegalArgumentException {

    public PatientNotFoundException() {
        super("Patient not found");
    }
}
