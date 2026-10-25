package patients.service;

import patients.model.Action;
import patients.model.Medication;
import patients.model.MedicationCatalog;
import patients.model.Patient;
import patients.model.PatientField;
import patients.model.User;
import patients.repository.Ids;
import patients.repository.PatientRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PatientService {

    private static final Comparator<Patient> BY_NAME = Comparator
            .comparing(Patient::lastName, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER))
            .thenComparing(Patient::firstName, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER))
            .thenComparing(Patient::id, Comparator.nullsFirst(Comparator.naturalOrder()));

    private final PatientRepository repository;
    private final Clock clock;

    public PatientService(PatientRepository repository) {
        this(repository, Clock.systemDefaultZone());
    }

    public PatientService(PatientRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public List<Patient> getAll(User user) {
        requirePermission(user, Action.VIEW_LIST);
        return repository.findAllForUser(user.id(), FieldPermissions.getViewableFields(user.profession())).stream()
                .filter(p -> p.hasMember(user.id()))
                .sorted(BY_NAME)
                .toList();
    }

    public Patient get(User user, String id) {
        return getAll(user).stream()
                .filter(p -> p.id().equals(id))
                .findFirst()
                .orElseThrow(PatientNotFoundException::new);
    }

    public Patient add(User user, Patient patient) {
        requirePermission(user, Action.ADD_PATIENT);
        validate(patient);
        validateMedications(patient.medications());
        Map<String, Boolean> careTeam = new LinkedHashMap<>(patient.careTeam());
        careTeam.put(user.id(), true);
        Patient toSave = patient.withCareTeam(careTeam);
        if (toSave.id() == null) {
            toSave = toSave.withId(Ids.newId());
        }
        repository.save(toSave);
        return toSave;
    }

    public Patient update(User user, Patient updatedPatient) {
        requirePermission(user, Action.EDIT_PATIENT);
        Patient existing = repository.findById(updatedPatient.id())
                .orElseThrow(PatientNotFoundException::new);
        if (!existing.hasMember(user.id())) {
            throw new SecurityException("You are not on this patient's care team");
        }

        for (PatientField field : PatientField.values()) {
            boolean changed = !Objects.equals(field.valueOf(existing), field.valueOf(updatedPatient));
            if (changed && !FieldPermissions.canEdit(user.profession(), field)) {
                throw new SecurityException("You do not have the permission to edit " + field.label);
            }
        }
        if (!Objects.equals(existing.careTeam(), updatedPatient.careTeam())) {
            throw new SecurityException("The care team can't be edited");
        }
        validate(updatedPatient);
        if (!Objects.equals(existing.medications(), updatedPatient.medications())) {
            validateMedications(updatedPatient.medications());
        }

        repository.save(updatedPatient);
        return updatedPatient;
    }

    private static void requirePermission(User user, Action action) {
        if (user == null) {
            throw new IllegalStateException("No user is logged in");
        }
        if (!ProfessionPermissions.isAllowed(user.profession(), action)) {
            throw new SecurityException(user.profession().label + " is not allowed to " + action.label);
        }
    }

    private void validate(Patient patient) {
        if (patient.firstName() == null || patient.firstName().isBlank()) {
            throw new IllegalArgumentException("First name can't be empty");
        }
        if (patient.lastName() == null || patient.lastName().isBlank()) {
            throw new IllegalArgumentException("Last name can't be empty");
        }
        if (patient.birthDate() == null) {
            throw new IllegalArgumentException("Date of birth is required");
        }
        if (patient.birthDate().isAfter(LocalDate.now(clock))) {
            throw new IllegalArgumentException("A date of birth can't be in the future");
        }
        if (!(patient.weight() > 0)) {
            throw new IllegalArgumentException("Weight must be greater than 0");
        }
        if (!(patient.height() > 0)) {
            throw new IllegalArgumentException("Height must be greater than 0");
        }
    }

    private static void validateMedications(List<Medication> medications) {
        for (Medication medication : medications) {
            if (medication == null || medication.name() == null || medication.name().isBlank()) {
                throw new IllegalArgumentException("The medication name can't be empty");
            }
            if (!MedicationCatalog.contains(medication.name())) {
                throw new IllegalArgumentException("Unknown medication: " + medication.name());
            }
            if (medication.dosage() == null || medication.dosage().isBlank()) {
                throw new IllegalArgumentException("The dosage of " + medication.name() + " can't be empty");
            }
            if (medication.frequency() == null || medication.frequency().isBlank()) {
                throw new IllegalArgumentException("The frequency of " + medication.name() + " can't be empty");
            }
        }
    }
}
