package patients.service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import patients.model.PatientField;
import patients.model.Profession;

public class FieldPermissions {

    private static final Map<Profession, Set<PatientField>> editableFields = new EnumMap<>(Profession.class);
    private static final Map<Profession, Set<PatientField>> viewableFields = new EnumMap<>(Profession.class);

    static {
        editableFields.put(
                Profession.DOCTOR,
                EnumSet.of(
                        PatientField.FIRST_NAME,
                        PatientField.LAST_NAME,
                        PatientField.DATE_OF_BIRTH,
                        PatientField.WEIGHT,
                        PatientField.HEIGHT,
                        PatientField.ADDRESS));
        editableFields.put(
                Profession.PHARMACIST,
                EnumSet.noneOf(
                        PatientField.class));
        editableFields.put(
                Profession.PHYSIOTHERAPIST,
                EnumSet.of(
                        PatientField.FIRST_NAME,
                        PatientField.LAST_NAME,
                        PatientField.WEIGHT,
                        PatientField.HEIGHT,
                        PatientField.ADDRESS));
        editableFields.put(
                Profession.DENTIST,
                EnumSet.of(
                        PatientField.FIRST_NAME,
                        PatientField.LAST_NAME,
                        PatientField.ADDRESS));

        viewableFields.put(
                Profession.DOCTOR,
                EnumSet.of(
                        PatientField.FIRST_NAME,
                        PatientField.LAST_NAME,
                        PatientField.DATE_OF_BIRTH,
                        PatientField.WEIGHT,
                        PatientField.HEIGHT,
                        PatientField.ADDRESS));
        viewableFields.put(
                Profession.PHARMACIST,
                EnumSet.of(
                        PatientField.FIRST_NAME,
                        PatientField.LAST_NAME,
                        PatientField.DATE_OF_BIRTH));
        viewableFields.put(Profession.PHYSIOTHERAPIST,
                EnumSet.of(
                        PatientField.FIRST_NAME,
                        PatientField.LAST_NAME,
                        PatientField.DATE_OF_BIRTH,
                        PatientField.WEIGHT,
                        PatientField.HEIGHT,
                        PatientField.ADDRESS));
        viewableFields.put(
                Profession.DENTIST,
                EnumSet.of(
                        PatientField.FIRST_NAME,
                        PatientField.LAST_NAME,
                        PatientField.DATE_OF_BIRTH,
                        PatientField.WEIGHT,
                        PatientField.HEIGHT,
                        PatientField.ADDRESS));
    }

    public static boolean canEdit(Profession profession, PatientField patientField) {
        return editableFields.get(profession).contains(patientField);
    }

    public static Set<PatientField> getEditableFields(Profession profession) {
        return editableFields.get(profession);
    }

    public static boolean canView(Profession profession, PatientField patientField) {
        return viewableFields.get(profession).contains(patientField);
    }

    public static Set<PatientField> getViewableFields(Profession profession) {
        return viewableFields.get(profession);
    }
}
