package patients.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import patients.model.PatientField;
import patients.model.Profession;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldPermissionsTest {

    @Test
    void editableFieldsMatchExpectedTable() {
        assertEquals(EnumSet.allOf(PatientField.class),
                FieldPermissions.getEditableFields(Profession.DOCTOR));
        assertEquals(EnumSet.noneOf(PatientField.class),
                FieldPermissions.getEditableFields(Profession.PHARMACIST));
        assertEquals(EnumSet.of(PatientField.FIRST_NAME, PatientField.LAST_NAME,
                        PatientField.WEIGHT, PatientField.HEIGHT, PatientField.ADDRESS),
                FieldPermissions.getEditableFields(Profession.PHYSIOTHERAPIST));
        assertEquals(EnumSet.of(PatientField.FIRST_NAME, PatientField.LAST_NAME, PatientField.ADDRESS),
                FieldPermissions.getEditableFields(Profession.DENTIST));
    }

    @Test
    void viewableFieldsMatchExpectedTable() {
        assertEquals(EnumSet.allOf(PatientField.class),
                FieldPermissions.getViewableFields(Profession.DOCTOR));
        assertEquals(EnumSet.of(PatientField.FIRST_NAME, PatientField.LAST_NAME, PatientField.DATE_OF_BIRTH),
                FieldPermissions.getViewableFields(Profession.PHARMACIST));
        assertEquals(EnumSet.allOf(PatientField.class),
                FieldPermissions.getViewableFields(Profession.PHYSIOTHERAPIST));
        assertEquals(EnumSet.allOf(PatientField.class),
                FieldPermissions.getViewableFields(Profession.DENTIST));
    }

    @ParameterizedTest
    @EnumSource(Profession.class)
    void editableFieldsAreAlsoViewable(Profession profession) {
        assertTrue(FieldPermissions.getViewableFields(profession)
                .containsAll(FieldPermissions.getEditableFields(profession)));
    }

    @ParameterizedTest
    @EnumSource(Profession.class)
    void canEditAndCanViewAgreeWithTheirSets(Profession profession) {
        for (PatientField field : PatientField.values()) {
            assertEquals(FieldPermissions.getEditableFields(profession).contains(field),
                    FieldPermissions.canEdit(profession, field));
            assertEquals(FieldPermissions.getViewableFields(profession).contains(field),
                    FieldPermissions.canView(profession, field));
        }
    }
}
