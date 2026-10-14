package patients.repository;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import patients.model.Patient;
import patients.model.PatientField;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Stores patients in a Firebase Realtime Database. Next to {@code /patients/<id>} it keeps the
 * index {@code /userPatients/<userId>/<patientId>: true}, because database rules can't filter a
 * read of {@code /patients}: a user has to read their own index and then fetch each patient.
 * A user who may not view every field is denied the whole patient, so they fetch their fields one by one.
 */
public class FirebasePatientRepository implements PatientRepository {

    private final FirebaseClient client;
    private final Gson gson = GsonFactory.builder().create();

    public FirebasePatientRepository(FirebaseClient client) {
        this.client = client;
    }

    @Override
    public List<Patient> findAll() {
        List<Patient> patients = new ArrayList<>();
        JsonElement tree = client.get("patients");
        if (tree.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : tree.getAsJsonObject().entrySet()) {
                patients.add(toPatient(entry.getKey(), entry.getValue()));
            }
        }
        return patients;
    }

    @Override
    public Optional<Patient> findById(String id) {
        JsonElement element = client.get("patients/" + id);
        return element.isJsonObject() ? Optional.of(toPatient(id, element)) : Optional.empty();
    }

    @Override
    public List<Patient> findAllForUser(String userId) {
        return findAllForUser(userId, EnumSet.allOf(PatientField.class));
    }

    @Override
    public List<Patient> findAllForUser(String userId, Set<PatientField> fields) {
        List<Patient> patients = new ArrayList<>();
        JsonElement index = client.get("userPatients/" + userId);
        if (index.isJsonObject()) {
            for (String patientId : index.getAsJsonObject().keySet()) {
                findById(patientId, fields)
                        .filter(p -> p.hasMember(userId))
                        .ifPresent(patients::add);
            }
        }
        return patients;
    }

    private Optional<Patient> findById(String id, Set<PatientField> fields) {
        if (fields.containsAll(EnumSet.allOf(PatientField.class))) {
            return findById(id);
        }
        String path = "patients/" + id;
        JsonElement careTeam = client.get(path + "/careTeam");
        if (!careTeam.isJsonObject()) {
            return Optional.empty();
        }
        JsonObject stored = new JsonObject();
        stored.add("careTeam", careTeam);
        for (PatientField field : fields) {
            JsonElement value = client.get(path + "/" + field.key);
            if (!value.isJsonNull()) {
                stored.add(field.key, value);
            }
        }
        return Optional.of(toPatient(id, stored));
    }

    @Override
    public void save(Patient patient) {
        if (patient.id() == null) {
            throw new IllegalArgumentException("Can't save a patient without an id");
        }
        String id = patient.id();
        String path = "patients/" + id;
        JsonObject stored = gson.toJsonTree(patient.withId(null)).getAsJsonObject();
        JsonElement existing = client.get(path);

        JsonObject update = new JsonObject();
        if (existing.isJsonObject()) {
            JsonObject old = existing.getAsJsonObject();
            Set<String> keys = new LinkedHashSet<>(old.keySet());
            keys.addAll(stored.keySet());
            for (String key : keys) {
                JsonElement value = valueOf(stored, key);
                if (!value.equals(valueOf(old, key))) {
                    update.add(path + "/" + key, value);
                }
            }
            JsonElement oldTeam = old.get("careTeam");
            if (oldTeam != null && oldTeam.isJsonObject()) {
                for (String userId : oldTeam.getAsJsonObject().keySet()) {
                    if (!patient.hasMember(userId)) {
                        update.add("userPatients/" + userId + "/" + id, JsonNull.INSTANCE);
                    }
                }
            }
        } else {
            update.add(path, stored);
        }
        for (String userId : patient.careTeam().keySet()) {
            if (patient.hasMember(userId)) {
                update.add("userPatients/" + userId + "/" + id, new JsonPrimitive(true));
            }
        }
        if (!update.isEmpty()) {
            client.patch("", update);
        }
    }

    private static JsonElement valueOf(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null ? JsonNull.INSTANCE : value;
    }

    private Patient toPatient(String id, JsonElement element) {
        return gson.fromJson(element, Patient.class).withId(id);
    }
}
