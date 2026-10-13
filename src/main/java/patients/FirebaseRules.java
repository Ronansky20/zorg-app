package patients;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import patients.model.Action;
import patients.model.PatientField;
import patients.model.Profession;
import patients.service.FieldPermissions;
import patients.service.ProfessionPermissions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class FirebaseRules {

    static final Map<PatientField, String> VALIDATIONS = new EnumMap<>(PatientField.class);

    static {
        VALIDATIONS.put(PatientField.FIRST_NAME, "newData.isString() && newData.val().length > 0");
        VALIDATIONS.put(PatientField.LAST_NAME, "newData.isString() && newData.val().length > 0");
        VALIDATIONS.put(PatientField.DATE_OF_BIRTH,
                "newData.isString() && newData.val().matches(/^[0-9]{4}-[0-9]{2}-[0-9]{2}$/)");
        VALIDATIONS.put(PatientField.WEIGHT, "newData.isNumber() && newData.val() > 0");
        VALIDATIONS.put(PatientField.HEIGHT, "newData.isNumber() && newData.val() > 0");
        VALIDATIONS.put(PatientField.ADDRESS, "newData.isString()");
    }

    static final Path FILE = Paths.get("database.rules.json");

    private static final String PROFESSION = "root.child('users').child(auth.uid).child('profession').val()";
    private static final String NEW_ROOT = "newData.parent().parent().parent()";

    public static void main(String[] args) throws IOException {
        Files.writeString(FILE, generate());
        System.out.println("Wrote " + FILE.toAbsolutePath());
    }

    public static String generate() {
        JsonObject rules = new JsonObject();
        rules.add("users", users());
        rules.add("patients", patients());
        rules.add("userPatients", userPatients());
        JsonObject root = new JsonObject();
        root.add("rules", rules);
        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(root) + "\n";
    }

    private static JsonObject users() {
        JsonObject users = new JsonObject();
        users.addProperty(".read", "auth != null");
        return users;
    }

    private static JsonObject patients() {
        JsonObject patient = new JsonObject();
        patient.addProperty(".read",
                "auth != null && (!data.exists() || data.child('careTeam').child(auth.uid).val() === true)");
        patient.addProperty(".write", "auth != null && !data.exists()"
                + " && newData.child('careTeam').child(auth.uid).val() === true"
                + " && " + professionIn(p -> ProfessionPermissions.isAllowed(p, Action.ADD_PATIENT)));
        patient.addProperty(".validate", "newData.hasChildren(['firstName', 'lastName', 'weight', 'height', 'careTeam'])");

        for (PatientField field : PatientField.values()) {
            JsonObject rule = new JsonObject();
            rule.addProperty(".write", "auth != null"
                    + " && data.parent().child('careTeam').child(auth.uid).val() === true"
                    + " && " + professionIn(p -> ProfessionPermissions.isAllowed(p, Action.EDIT_PATIENT)
                            && FieldPermissions.canEdit(p, field)));
            if (VALIDATIONS.containsKey(field)) {
                rule.addProperty(".validate", VALIDATIONS.get(field));
            }
            patient.add(field.key, rule);
        }
        patient.getAsJsonObject("medications").add("$index", medication());

        JsonObject member = new JsonObject();
        member.addProperty(".validate", "newData.isBoolean()");
        JsonObject careTeam = new JsonObject();
        careTeam.add("$userId", member);
        patient.add("careTeam", careTeam);
        patient.add("$other", rejectAll());

        JsonObject patients = new JsonObject();
        patients.add("$patientId", patient);
        return patients;
    }

    private static JsonObject medication() {
        JsonObject medication = new JsonObject();
        medication.addProperty(".validate", "newData.hasChildren(['name'])");
        for (String key : List.of("name", "dosage", "frequency")) {
            JsonObject rule = new JsonObject();
            rule.addProperty(".validate", "newData.isString()");
            medication.add(key, rule);
        }
        medication.add("$other", rejectAll());
        return medication;
    }

    private static JsonObject userPatients() {
        String patientTeam = NEW_ROOT + ".child('patients').child($patientId).child('careTeam')";
        JsonObject entry = new JsonObject();
        entry.addProperty(".write", "auth != null"
                + " && " + patientTeam + ".child(auth.uid).val() === true"
                + " && newData.exists() === (" + patientTeam + ".child($userId).val() === true)");
        entry.addProperty(".validate", "newData.val() === true");

        JsonObject user = new JsonObject();
        user.addProperty(".read", "auth != null && auth.uid === $userId");
        user.add("$patientId", entry);

        JsonObject userPatients = new JsonObject();
        userPatients.add("$userId", user);
        return userPatients;
    }

    private static JsonObject rejectAll() {
        JsonObject reject = new JsonObject();
        reject.addProperty(".validate", false);
        return reject;
    }

    private static String professionIn(Predicate<Profession> allowed) {
        List<String> checks = new ArrayList<>();
        for (Profession profession : Profession.values()) {
            if (allowed.test(profession)) {
                checks.add(PROFESSION + " === '" + profession.name() + "'");
            }
        }
        return checks.isEmpty() ? "false" : "(" + String.join(" || ", checks) + ")";
    }
}
