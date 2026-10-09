package patients.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import patients.model.Action;
import patients.model.Profession;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfessionPermissionsTest {

    @ParameterizedTest
    @EnumSource(value = Profession.class, names = "PHARMACIST", mode = EnumSource.Mode.EXCLUDE)
    void doctorPhysiotherapistAndDentistCanViewAndAddPatients(Profession profession) {
        assertTrue(ProfessionPermissions.isAllowed(profession, Action.VIEW_LIST));
        assertTrue(ProfessionPermissions.isAllowed(profession, Action.ADD_PATIENT));
    }

    @Test
    void pharmacistHasNoPermissions() {
        assertFalse(ProfessionPermissions.isAllowed(Profession.PHARMACIST, Action.VIEW_LIST));
        assertFalse(ProfessionPermissions.isAllowed(Profession.PHARMACIST, Action.ADD_PATIENT));
        assertTrue(ProfessionPermissions.getAllowedActions(Profession.PHARMACIST).isEmpty());
    }

    @ParameterizedTest
    @EnumSource(Profession.class)
    void getAllowedActionsAgreesWithIsAllowed(Profession profession) {
        for (Action action : Action.values()) {
            boolean inSet = ProfessionPermissions.getAllowedActions(profession).contains(action);
            assertTrue(inSet == ProfessionPermissions.isAllowed(profession, action));
        }
    }
}
