package patients.service;

import patients.model.Patient;
import patients.model.PatientField;
import patients.model.Profession;
import patients.model.Action;
import patients.repository.PatientRepository;

import java.util.ArrayList;
import java.util.List;

public class PatientService {

    private final PatientRepository repository;
    private final List<Patient> patients = new ArrayList<>();
    private Profession currentProfession;

    private void requirePermission(Action action) {
        if (currentProfession == null) {
            throw new IllegalStateException("No user is logged in");
        }
        if (!ProfessionPermissions.isAllowed(currentProfession, action)) {
            throw new SecurityException(currentProfession.label + " is not allowed to " + action.label);
        }
    }

    public void login(Profession profession) {
        this.currentProfession = profession;
    }

    public PatientService(PatientRepository repository) {
        this.repository = repository;
    }

    public void load() {
        patients.clear();
        patients.addAll(repository.loadAll());
    }

    public void add(Patient patient) {
        requirePermission(Action.ADD_PATIENT);
        patients.add(patient);
    }

    public void update(Patient oldPatient, Patient updatedPatient, PatientField patientField) {
        requirePermission(Action.EDIT_PATIENT);
        if (!FieldPermissions.canEdit(currentProfession, patientField)) {
            throw new SecurityException("You do not have the permission to edit " + patientField.label);
        }
        int patientPosition = patients.indexOf(oldPatient);
        if (patientPosition == -1) {
            throw new IllegalArgumentException("Patient not found");
        } else {
            patients.set(patientPosition, updatedPatient);
        }
    }

    public List<Patient> getAll() {
        requirePermission(Action.VIEW_LIST);
        return List.copyOf(patients);
    }

    public void save() {
        repository.saveAll(patients);
    }
}