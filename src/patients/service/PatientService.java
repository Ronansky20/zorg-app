package patients.service;

import patients.model.Patient;
import patients.repository.PatientRepository;

import java.util.ArrayList;
import java.util.List;

public class PatientService {

    private final PatientRepository repository;
    private final List<Patient> patients = new ArrayList<>();

    public PatientService(PatientRepository repository) {
        this.repository = repository;
    }

    public void load() {
        patients.clear();
        patients.addAll(repository.loadAll());
    }

    public void add(Patient patient) {
        patients.add(patient);
    }

    public List<Patient> getAll() {
        return List.copyOf(patients);
    }

    public void save() {
        repository.saveAll(patients);
    }
}