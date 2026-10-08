package patients.service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import patients.model.Action;
import patients.model.Profession;

public class ProfessionPermissions {

    private static final Map<Profession, Set<Action>> allowedActions = new EnumMap<>(Profession.class);

    static {
        allowedActions.put(Profession.DOCTOR, EnumSet.of(Action.VIEW_LIST, Action.ADD_PATIENT, Action.EDIT_PATIENT));
        allowedActions.put(Profession.PHARMACIST, EnumSet.of(Action.VIEW_LIST));
        allowedActions.put(Profession.PHYSIOTHERAPIST, EnumSet.of(Action.VIEW_LIST, Action.EDIT_PATIENT));
        allowedActions.put(Profession.DENTIST, EnumSet.of(Action.VIEW_LIST, Action.EDIT_PATIENT));
    }

    public static boolean isAllowed(Profession profession, Action action) {
        return allowedActions.get(profession).contains(action);
    }

    public static Set<Action> getAllowedActions(Profession profession) {
        return allowedActions.get(profession);
    }
}
