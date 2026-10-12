package patients.service;

import patients.model.Patient;
import patients.repository.PatientRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Simple in-memory stand-in for a real repository, used to test {@link PatientService} in isolation. */
class InMemoryPatientRepository implements PatientRepository {

    private final Map<String, Patient> stored = new LinkedHashMap<>();
    int saveCalls = 0;

    InMemoryPatientRepository() {}

    InMemoryPatientRepository(List<Patient> initial) {
        for (Patient patient : initial) {
            stored.put(patient.id(), patient);
        }
    }

    @Override
    public List<Patient> findAll() {
        return new ArrayList<>(stored.values());
    }

    @Override
    public Optional<Patient> findById(String id) {
        return Optional.ofNullable(stored.get(id));
    }

    @Override
    public void save(Patient patient) {
        if (patient.id() == null) {
            throw new IllegalArgumentException("Can't save a patient without an id");
        }
        saveCalls++;
        stored.put(patient.id(), patient);
    }

    List<Patient> getStored() {
        return new ArrayList<>(stored.values());
    }
}
