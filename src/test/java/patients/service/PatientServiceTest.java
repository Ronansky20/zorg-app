package patients.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import patients.model.Medication;
import patients.model.Patient;
import patients.model.PatientField;
import patients.model.Profession;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatientServiceTest {

    private InMemoryPatientRepository repository;
    private PatientService service;

    private static Patient patient(String id, String firstName) {
        return new Patient(id, firstName, "Doe", LocalDate.of(1995, 6, 15), 70.0, 1.8, "Elm Street 58",
                new Patient.Specialists("Anon", "Anon", "Anon", "Anon"), List.of());
    }

    private static Patient patient(String firstName) {
        return patient(firstName.toLowerCase(), firstName);
    }

    // Changes exactly one field, so update() sees a difference in that field only.
    private static Patient changeField(Patient patient, PatientField field) {
        return switch (field) {
            case FIRST_NAME -> patient.withFirstName(patient.firstName() + "x");
            case LAST_NAME -> patient.withLastName(patient.lastName() + "x");
            case DATE_OF_BIRTH -> patient.withBirthDate(patient.birthDate().minusDays(1));
            case WEIGHT -> patient.withWeight(patient.weight() + 1);
            case HEIGHT -> patient.withHeight(patient.height() + 0.1);
            case ADDRESS -> patient.withAddress(patient.address() + "x");
            case MEDICATION -> patient.withMedications(List.of(new Medication("Paracetamol", "500 mg", "3x daily")));
        };
    }

    private void startWith(Patient... initial) {
        repository = new InMemoryPatientRepository(List.of(initial));
        service = new PatientService(repository);
        service.login(Profession.DOCTOR);
        service.load();
    }

    @BeforeEach
    void setUp() {
        repository = new InMemoryPatientRepository();
        service = new PatientService(repository);
        service.login(Profession.DOCTOR);
    }

    @Test
    void getAllIsEmptyBeforeLoading() {
        assertTrue(service.getAll().isEmpty());
    }

    @Test
    void loadPopulatesPatientsFromRepository() {
        startWith(patient("John"), patient("Jane"));

        assertEquals(2, service.getAll().size());
    }

    @Test
    void loadSortsByLastNameThenFirstName() {
        Patient zed = patient("zed", "John").withLastName("Zed");
        startWith(zed, patient("John"), patient("Jane"));

        assertEquals(List.of(patient("Jane"), patient("John"), zed), service.getAll());
    }

    @Test
    void loadReplacesPreviouslyLoadedPatients() {
        startWith(patient("John"));

        repository.save(patient("John").withFirstName("Johnny"));
        service.load();

        assertEquals(List.of(patient("John").withFirstName("Johnny")), service.getAll());
    }

    @Test
    void addAssignsAnIdAndSavesImmediately() {
        service.add(patient(null, "John"));

        Patient added = service.getAll().get(0);
        assertNotNull(added.id());
        assertEquals(1, repository.saveCalls);
        assertEquals(List.of(added), repository.getStored());
    }

    @Test
    void addKeepsAnExistingId() {
        service.add(patient("id-1", "John"));

        assertEquals("id-1", repository.getStored().get(0).id());
    }

    @Test
    void addAppendsToInMemoryList() {
        service.add(patient("John"));
        service.add(patient("Jane"));

        assertEquals(List.of(patient("John"), patient("Jane")), service.getAll());
    }

    @Test
    void getAllReturnsAnImmutableSnapshot() {
        service.add(patient("John"));
        List<Patient> snapshot = service.getAll();

        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(patient("Jane")));
    }

    @Test
    void addAndGetAllRequireALoggedInUser() {
        service = new PatientService(repository);

        assertThrows(IllegalStateException.class, () -> service.add(patient("John")));
        assertThrows(IllegalStateException.class, () -> service.getAll());
    }

    @Test
    void addIsRejectedForProfessionsOtherThanDoctor() {
        for (Profession profession : List.of(Profession.PHARMACIST, Profession.PHYSIOTHERAPIST, Profession.DENTIST)) {
            service.login(profession);

            assertThrows(SecurityException.class, () -> service.add(patient("John")));
        }
        assertEquals(0, repository.saveCalls);
    }

    @Test
    void addRejectsInvalidPatients() {
        assertThrows(IllegalArgumentException.class, () -> service.add(patient("John").withFirstName(" ")));
        assertThrows(IllegalArgumentException.class, () -> service.add(patient("John").withLastName("")));
        assertThrows(IllegalArgumentException.class, () -> service.add(patient("John").withHeight(0)));
        assertThrows(IllegalArgumentException.class, () -> service.add(patient("John").withWeight(-1)));
        assertEquals(0, repository.saveCalls);
    }

    @Test
    void updateSavesAndReplacesTheCachedPatient() {
        startWith(patient("John"), patient("Jane"));
        Patient updated = patient("John").withAddress("Oak Lane 1");

        service.update(updated);

        assertEquals(updated, repository.findById("john").orElseThrow());
        assertTrue(service.getAll().contains(updated));
        assertEquals(2, service.getAll().size());
    }

    @Test
    void updateOfUnknownPatientIsRejected() {
        startWith(patient("John"));

        assertThrows(IllegalArgumentException.class, () -> service.update(patient("Jane")));
    }

    @Test
    void eachProfessionCanChangeExactlyItsEditableFields() {
        for (Profession profession : Profession.values()) {
            if (profession == Profession.PHARMACIST) {
                continue; // can't edit at all, see below
            }
            for (PatientField field : PatientField.values()) {
                startWith(patient("John"));
                service.login(profession);
                Patient updated = changeField(patient("John"), field);

                if (FieldPermissions.canEdit(profession, field)) {
                    assertDoesNotThrow(() -> service.update(updated), profession + " " + field);
                    assertEquals(updated, repository.findById("john").orElseThrow());
                } else {
                    assertThrows(SecurityException.class, () -> service.update(updated), profession + " " + field);
                    assertEquals(patient("John"), repository.findById("john").orElseThrow());
                }
            }
        }
    }

    @Test
    void pharmacistCanNotEdit() {
        startWith(patient("John"));
        service.login(Profession.PHARMACIST);

        assertThrows(SecurityException.class,
                () -> service.update(patient("John").withAddress("Oak Lane 1")));
    }

    @Test
    void specialistsCanNotBeChanged() {
        Patient original = patient("John");
        startWith(original);
        Patient updated = new Patient(original.id(), original.firstName(), original.lastName(),
                original.birthDate(), original.weight(), original.height(), original.address(),
                new Patient.Specialists("Someone else", "Anon", "Anon", "Anon"), original.medications());

        assertThrows(SecurityException.class, () -> service.update(updated));
    }

    @Test
    void updateRejectsInvalidValues() {
        startWith(patient("John"));

        assertThrows(IllegalArgumentException.class, () -> service.update(patient("John").withHeight(0)));
        assertThrows(IllegalArgumentException.class, () -> service.update(patient("John").withFirstName("")));
        assertEquals(0, repository.saveCalls);
    }

    @Test
    void doctorCanUpdateMedication() {
        startWith(patient("John"));
        Patient updated = patient("John").withMedications(List.of(new Medication("Paracetamol", "500 mg", "3x daily")));

        service.update(updated);

        assertEquals(List.of(updated), service.getAll());
    }

    @Test
    void medicationUpdateIsRejectedForProfessionsOtherThanDoctor() {
        startWith(patient("John"));
        Patient updated = patient("John").withMedications(List.of(new Medication("Paracetamol", "500 mg", "3x daily")));

        for (Profession profession : List.of(Profession.PHARMACIST, Profession.PHYSIOTHERAPIST, Profession.DENTIST)) {
            service.login(profession);

            assertThrows(SecurityException.class, () -> service.update(updated));
        }
        assertEquals(List.of(patient("John")), service.getAll());
    }

    @Test
    void getAllIsAllowedForEveryProfession() {
        service.add(patient("John"));

        for (Profession profession : Profession.values()) {
            service.login(profession);

            assertEquals(List.of(patient("John")), service.getAll());
        }
    }
}
