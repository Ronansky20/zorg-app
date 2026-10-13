package patients.service;

import patients.model.Patient;
import patients.model.PatientField;
import patients.model.Profession;
import patients.model.Action;
import patients.repository.Ids;
import patients.repository.PatientRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class PatientService {

    private static final Comparator<Patient> BY_NAME = Comparator
            .comparing(Patient::lastName, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER))
            .thenComparing(Patient::firstName, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER))
            .thenComparing(Patient::id, Comparator.nullsFirst(Comparator.naturalOrder()));

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
        patients.addAll(repository.findAll());
        patients.sort(BY_NAME);
    }

    public void add(Patient patient) {
        requirePermission(Action.ADD_PATIENT);
        validate(patient);
        Patient toSave = patient.id() == null ? patient.withId(Ids.newId()) : patient;
        repository.save(toSave);
        patients.add(toSave);
    }

    public void update(Patient updatedPatient) {
        requirePermission(Action.EDIT_PATIENT);
        Patient existing = repository.findById(updatedPatient.id())
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));

        for (PatientField field : PatientField.values()) {
            boolean changed = !Objects.equals(field.valueOf(existing), field.valueOf(updatedPatient));
            if (changed && !FieldPermissions.canEdit(currentProfession, field)) {
                throw new SecurityException("You do not have the permission to edit " + field.label);
            }
        }
        if (!Objects.equals(existing.specialists(), updatedPatient.specialists())) {
            throw new SecurityException("Specialists can't be edited");
        }
        validate(updatedPatient);

        repository.save(updatedPatient);
        patients.replaceAll(p -> p.id().equals(updatedPatient.id()) ? updatedPatient : p);
    }

    public List<Patient> getAll() {
        requirePermission(Action.VIEW_LIST);
        return List.copyOf(patients);
    }

    private static void validate(Patient patient) {
        if (patient.firstName() == null || patient.firstName().isBlank()) {
            throw new IllegalArgumentException("First name can't be empty");
        }
        if (patient.lastName() == null || patient.lastName().isBlank()) {
            throw new IllegalArgumentException("Last name can't be empty");
        }
        if (!(patient.weight() > 0)) {
            throw new IllegalArgumentException("Weight must be greater than 0");
        }
        if (!(patient.height() > 0)) {
            throw new IllegalArgumentException("Height must be greater than 0");
        }
    }
}
