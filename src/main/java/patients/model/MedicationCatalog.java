package patients.model;

import java.util.Arrays;

public enum MedicationCatalog {
    PARACETAMOL("Paracetamol", MedicationType.PILL),
    IBUPROFEN("Ibuprofen", MedicationType.PILL),
    AMOXICILLIN("Amoxicillin", MedicationType.PILL),
    OMEPRAZOLE("Omeprazole", MedicationType.PILL),
    METFORMIN("Metformin", MedicationType.PILL),
    SIMVASTATIN("Simvastatin", MedicationType.PILL),
    AMLODIPINE("Amlodipine", MedicationType.PILL),
    LEVOTHYROXINE("Levothyroxine", MedicationType.PILL),
    INSULIN("Insulin", MedicationType.INJECTION),
    HEPARIN("Heparin", MedicationType.INJECTION),
    VITAMIN_B12("Vitamin B12", MedicationType.INJECTION),
    ADRENALINE("Adrenaline", MedicationType.INJECTION),
    CEFTRIAXONE("Ceftriaxone", MedicationType.INJECTION),
    ENOXAPARIN("Enoxaparin", MedicationType.INJECTION),
    MORPHINE("Morphine", MedicationType.INJECTION),
    INFLUENZA_VACCINE("Influenza vaccine", MedicationType.INJECTION),
    HYDROCORTISONE("Hydrocortisone", MedicationType.OINTMENT),
    FUSIDIC_ACID("Fusidic acid", MedicationType.OINTMENT),
    BETAMETHASONE("Betamethasone", MedicationType.OINTMENT),
    MUPIROCIN("Mupirocin", MedicationType.OINTMENT),
    CLOTRIMAZOLE("Clotrimazole", MedicationType.OINTMENT),
    MICONAZOLE("Miconazole", MedicationType.OINTMENT),
    ZINC_OXIDE("Zinc oxide", MedicationType.OINTMENT),
    SILVER_SULFADIAZINE("Silver sulfadiazine", MedicationType.OINTMENT);

    public final String label;
    public final MedicationType type;

    private MedicationCatalog(String label, MedicationType type) {
        this.label = label;
        this.type = type;
    }

    public static boolean contains(String name) {
        return Arrays.stream(values()).anyMatch(m -> m.label.equals(name));
    }
}
