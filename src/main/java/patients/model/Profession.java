package patients.model;

public enum Profession {
    DOCTOR("Doctor"),
    PHARMACIST("Pharmacist"),
    PHYSIOTHERAPIST("Physiotherapist"),
    DENTIST("Dentist");

    public final String label;

    private Profession(String label) {
        this.label = label;
    }
}