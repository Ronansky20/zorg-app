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

}
