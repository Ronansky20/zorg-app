package patients.service;

import patients.model.Patient;
import patients.repository.PatientRepository;

import java.util.ArrayList;
import java.util.List;

/** Simple in-memory stand-in for a real repository, used to test {@link PatientService} in isolation. */
class InMemoryPatientRepository implements PatientRepository {

    private List<Patient> stored = new ArrayList<>();
    int loadAllCalls = 0;
    int saveAllCalls = 0;

    InMemoryPatientRepository() {}

    InMemoryPatientRepository(List<Patient> initial) {
        this.stored = new ArrayList<>(initial);
    }

    @Override
    public List<Patient> loadAll() {
        loadAllCalls++;
        return new ArrayList<>(stored);
    }

    @Override
    public void saveAll(List<Patient> patients) {
        saveAllCalls++;
        this.stored = new ArrayList<>(patients);
    }

    List<Patient> getStored() {
        return stored;
    }
}
