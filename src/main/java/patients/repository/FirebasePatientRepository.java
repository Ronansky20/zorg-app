package patients.repository;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import patients.model.Patient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Stores patients in a Firebase Realtime Database. Next to {@code /patients/<id>} it keeps the
 * index {@code /userPatients/<userId>/<patientId>: true}, because database rules can't filter a
 * read of {@code /patients}: a user has to read their own index and then fetch each patient.
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
        List<Patient> patients = new ArrayList<>();
        JsonElement index = client.get("userPatients/" + userId);
        if (index.isJsonObject()) {
            for (String patientId : index.getAsJsonObject().keySet()) {
                findById(patientId)
                        .filter(p -> p.hasMember(userId))
                        .ifPresent(patients::add);
            }
        }
        return patients;
    }

    @Override
    public void save(Patient patient) {
        if (patient.id() == null) {
            throw new IllegalArgumentException("Can't save a patient without an id");
        }
        String id = patient.id();

        JsonObject update = new JsonObject();
        update.add("patients/" + id, gson.toJsonTree(patient.withId(null)));
        JsonElement oldTeam = client.get("patients/" + id + "/careTeam");
        if (oldTeam.isJsonObject()) {
            for (String userId : oldTeam.getAsJsonObject().keySet()) {
                if (!patient.hasMember(userId)) {
                    update.add("userPatients/" + userId + "/" + id, JsonNull.INSTANCE);
                }
            }
        }
        for (String userId : patient.careTeam().keySet()) {
            if (patient.hasMember(userId)) {
                update.add("userPatients/" + userId + "/" + id, new JsonPrimitive(true));
            }
        }
        client.patch("", update);
    }

    private Patient toPatient(String id, JsonElement element) {
        return gson.fromJson(element, Patient.class).withId(id);
    }
}
