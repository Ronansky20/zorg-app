package patients.repository;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import patients.model.Patient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JsonPatientRepository implements PatientRepository {

    private static final String PATIENTS_KEY = "patients";
    private static final Type PATIENTS_TYPE =
            new TypeToken<LinkedHashMap<String, Patient>>() {}.getType();

    private final Path file;
    private final Gson gson = GsonFactory.builder()
            .setPrettyPrinting()
            .create();

    public JsonPatientRepository(Path file) {
        this.file = file;
    }

    @Override
    public List<Patient> findAll() {
        List<Patient> patients = new ArrayList<>();
        for (Map.Entry<String, Patient> entry : readFile().entrySet()) {
            patients.add(entry.getValue().withId(entry.getKey()));
        }
        return patients;
    }

    @Override
    public Optional<Patient> findById(String id) {
        return Optional.ofNullable(readFile().get(id)).map(p -> p.withId(id));
    }

    @Override
    public void save(Patient patient) {
        if (patient.id() == null) {
            throw new IllegalArgumentException("Can't save a patient without an id");
        }

        Map<String, Patient> patients = readFile();
        patients.put(patient.id(), patient.withId(null));
        try (Writer writer = Files.newBufferedWriter(file)) {
            gson.toJson(Map.of(PATIENTS_KEY, patients), writer);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save " + file, e);
        }
    }

    private Map<String, Patient> readFile() {
        return JsonFiles.readSection(gson, file, PATIENTS_KEY, PATIENTS_TYPE);
    }
}
