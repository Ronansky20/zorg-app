package patients;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import patients.model.Patient;
import patients.model.PatientField;

import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FirebaseRulesTest {

    private static JsonObject patientRules() {
        return JsonParser.parseString(FirebaseRules.generate()).getAsJsonObject()
                .getAsJsonObject("rules").getAsJsonObject("patients").getAsJsonObject("$patientId");
    }

    private static String writeRule(String key) {
        return patientRules().getAsJsonObject(key).get(".write").getAsString();
    }

    @Test
    void committedRulesFileIsUpToDate() throws IOException {
        assertEquals(FirebaseRules.generate(), Files.readString(FirebaseRules.FILE),
                "database.rules.json is out of date, run ./gradlew generateFirebaseRules");
    }

    @Test
    void everyPatientFieldHasARuleForAStoredKey() {
        Set<String> components = Arrays.stream(Patient.class.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());

        for (PatientField field : PatientField.values()) {
            String key = FirebaseRules.FIELDS.get(field).key();
            assertTrue(components.contains(key), field + " maps to " + key + ", which Patient doesn't have");
            assertTrue(patientRules().has(key), "no rule for " + key);
        }
    }

    @Test
    void fieldWriteRulesFollowFieldPermissions() {
        assertTrue(writeRule("weight").contains("'PHYSIOTHERAPIST'"));
        assertFalse(writeRule("weight").contains("'DENTIST'"));
        assertTrue(writeRule("address").contains("'DENTIST'"));
        assertTrue(writeRule("medications").contains("'DOCTOR'"));
        assertFalse(writeRule("medications").contains("'PHYSIOTHERAPIST'"));
        for (PatientField field : PatientField.values()) {
            assertFalse(writeRule(FirebaseRules.FIELDS.get(field).key()).contains("'PHARMACIST'"), field.toString());
        }
    }

    @Test
    void onlyProfessionsThatMayAddPatientsCanCreateThem() {
        String create = patientRules().get(".write").getAsString();

        assertTrue(create.contains("!data.exists()"));
        assertTrue(create.contains("'DOCTOR'"));
        assertFalse(create.contains("'DENTIST'"));
        assertFalse(create.contains("'PHARMACIST'"));
    }

    @Test
    void careTeamHasNoWriteRuleOfItsOwn() {
        assertFalse(patientRules().getAsJsonObject("careTeam").has(".write"));
    }

    @Test
    void usersAreReadOnly() {
        JsonObject users = JsonParser.parseString(FirebaseRules.generate()).getAsJsonObject()
                .getAsJsonObject("rules").getAsJsonObject("users");

        assertEquals("auth != null", users.get(".read").getAsString());
        assertFalse(users.has(".write"));
    }
}
