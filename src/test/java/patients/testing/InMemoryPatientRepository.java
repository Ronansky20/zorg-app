package patients.testing;

import patients.model.Patient;
import patients.repository.PatientRepository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryPatientRepository implements PatientRepository {

    private final Map<String, Patient> stored = new LinkedHashMap<>();
    public int saveCalls = 0;
    public boolean broken = false;

    public InMemoryPatientRepository() {}

    public InMemoryPatientRepository(List<Patient> initial) {
        for (Patient patient : initial) {
            stored.put(patient.id(), patient);
        }
    }

    @Override
    public synchronized List<Patient> findAll() {
        if (broken) {
            throw new UncheckedIOException(new IOException("Repository is broken"));
        }
        return new ArrayList<>(stored.values());
    }

    @Override
    public synchronized Optional<Patient> findById(String id) {
        return Optional.ofNullable(stored.get(id));
    }

    @Override
    public synchronized void save(Patient patient) {
        if (patient.id() == null) {
            throw new IllegalArgumentException("Can't save a patient without an id");
        }
        saveCalls++;
        stored.put(patient.id(), patient);
    }

    public synchronized List<Patient> getStored() {
        return new ArrayList<>(stored.values());
    }

    public synchronized void clear() {
        stored.clear();
        saveCalls = 0;
        broken = false;
    }
}
