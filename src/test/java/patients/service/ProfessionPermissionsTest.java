package patients.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import patients.model.Action;
import patients.model.Profession;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfessionPermissionsTest {

    @ParameterizedTest
    @EnumSource(Profession.class)
    void everyProfessionCanViewTheList(Profession profession) {
        assertTrue(ProfessionPermissions.isAllowed(profession, Action.VIEW_LIST));
    }

    @Test
    void onlyDoctorCanAddPatients() {
        assertTrue(ProfessionPermissions.isAllowed(Profession.DOCTOR, Action.ADD_PATIENT));
        assertFalse(ProfessionPermissions.isAllowed(Profession.PHARMACIST, Action.ADD_PATIENT));
        assertFalse(ProfessionPermissions.isAllowed(Profession.PHYSIOTHERAPIST, Action.ADD_PATIENT));
        assertFalse(ProfessionPermissions.isAllowed(Profession.DENTIST, Action.ADD_PATIENT));
    }

    @Test
    void allButPharmacistCanEditPatients() {
        assertTrue(ProfessionPermissions.isAllowed(Profession.DOCTOR, Action.EDIT_PATIENT));
        assertFalse(ProfessionPermissions.isAllowed(Profession.PHARMACIST, Action.EDIT_PATIENT));
        assertTrue(ProfessionPermissions.isAllowed(Profession.PHYSIOTHERAPIST, Action.EDIT_PATIENT));
        assertTrue(ProfessionPermissions.isAllowed(Profession.DENTIST, Action.EDIT_PATIENT));
    }

    @Test
    void getAllowedActionsMatchesExpectedTable() {
        assertEquals(EnumSet.allOf(Action.class),
                ProfessionPermissions.getAllowedActions(Profession.DOCTOR));
        assertEquals(EnumSet.of(Action.VIEW_LIST),
                ProfessionPermissions.getAllowedActions(Profession.PHARMACIST));
        assertEquals(EnumSet.of(Action.VIEW_LIST, Action.EDIT_PATIENT),
                ProfessionPermissions.getAllowedActions(Profession.PHYSIOTHERAPIST));
        assertEquals(EnumSet.of(Action.VIEW_LIST, Action.EDIT_PATIENT),
                ProfessionPermissions.getAllowedActions(Profession.DENTIST));
    }

    @ParameterizedTest
    @EnumSource(Profession.class)
    void getAllowedActionsAgreesWithIsAllowed(Profession profession) {
        for (Action action : Action.values()) {
            boolean inSet = ProfessionPermissions.getAllowedActions(profession).contains(action);
            assertEquals(inSet, ProfessionPermissions.isAllowed(profession, action));
        }
    }
}
