package patients.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import patients.model.Medication;
import patients.model.Patient;
import patients.model.PatientField;
import patients.model.Profession;
import patients.model.User;
import patients.testing.InMemoryPatientRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatientServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC);
    private static final User OUTSIDER = new User("outsider", "Dr. Outsider", Profession.DOCTOR);
    private static final User DOCTOR = user(Profession.DOCTOR);

    private InMemoryPatientRepository repository;
    private PatientService service;

    private static User user(Profession profession) {
        return new User(profession.name().toLowerCase(), profession.label, profession);
    }

    private static Map<String, Boolean> teamOfEveryProfession() {
        Map<String, Boolean> careTeam = new LinkedHashMap<>();
        for (Profession profession : Profession.values()) {
            careTeam.put(user(profession).id(), true);
        }
        return careTeam;
    }

    private static Patient patient(String id, String firstName) {
        return new Patient(id, firstName, "Doe", LocalDate.of(1995, 6, 15), 70.0, 1.8, "Elm Street 58",
                teamOfEveryProfession(), List.of());
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
        service = new PatientService(repository, CLOCK);
    }

    @BeforeEach
    void setUp() {
        startWith();
    }

    @Test
    void getAllIsEmptyWithoutPatients() {
        assertTrue(service.getAll(DOCTOR).isEmpty());
    }

    @Test
    void getAllReadsPatientsFromRepository() {
        startWith(patient("John"), patient("Jane"));

        assertEquals(2, service.getAll(DOCTOR).size());
    }

    @Test
    void getAllSortsByLastNameThenFirstName() {
        Patient zed = patient("zed", "John").withLastName("Zed");
        startWith(zed, patient("John"), patient("Jane"));

        assertEquals(List.of(patient("Jane"), patient("John"), zed), service.getAll(DOCTOR));
    }

    @Test
    void getAllOnlyReadsTheFieldsTheUserMayView() {
        service.getAll(user(Profession.PHARMACIST));

        assertEquals(FieldPermissions.getViewableFields(Profession.PHARMACIST), repository.requestedFields);
    }

    @Test
    void getAllSeesChangesSavedThroughTheRepository() {
        startWith(patient("John"));

        repository.save(patient("John").withFirstName("Johnny"));

        assertEquals(List.of(patient("John").withFirstName("Johnny")), service.getAll(DOCTOR));
    }

    @Test
    void addAssignsAnIdAndSavesImmediately() {
        Patient added = service.add(DOCTOR, patient(null, "John"));

        assertNotNull(added.id());
        assertEquals(1, repository.saveCalls);
        assertEquals(List.of(added), repository.getStored());
    }

    @Test
    void addKeepsAnExistingId() {
        service.add(DOCTOR, patient("id-1", "John"));

        assertEquals("id-1", repository.getStored().get(0).id());
    }

    @Test
    void addedPatientsShowUpInGetAll() {
        service.add(DOCTOR, patient("John"));
        service.add(DOCTOR, patient("Jane"));

        assertEquals(List.of(patient("Jane"), patient("John")), service.getAll(DOCTOR));
    }

    @Test
    void getAllReturnsAnImmutableList() {
        service.add(DOCTOR, patient("John"));
        List<Patient> patients = service.getAll(DOCTOR);

        assertThrows(UnsupportedOperationException.class, () -> patients.add(patient("Jane")));
    }

    @Test
    void everyMethodRequiresALoggedInUser() {
        startWith(patient("John"));

        assertThrows(IllegalStateException.class, () -> service.add(null, patient("Jane")));
        assertThrows(IllegalStateException.class, () -> service.getAll(null));
        assertThrows(IllegalStateException.class, () -> service.get(null, "john"));
        assertThrows(IllegalStateException.class, () -> service.update(null, patient("John")));
    }

    @Test
    void addIsRejectedForProfessionsOtherThanDoctor() {
        for (Profession profession : List.of(Profession.PHARMACIST, Profession.PHYSIOTHERAPIST, Profession.DENTIST)) {
            assertThrows(SecurityException.class, () -> service.add(user(profession), patient("John")));
        }
        assertEquals(0, repository.saveCalls);
    }

    @Test
    void addRejectsInvalidPatients() {
        List<Patient> invalid = List.of(
                patient("John").withFirstName(" "),
                patient("John").withLastName(""),
                patient("John").withHeight(0),
                patient("John").withWeight(-1),
                patient("John").withBirthDate(null),
                patient("John").withBirthDate(LocalDate.of(2026, 1, 2)),
                patient("John").withMedications(List.of(new Medication(" ", "500 mg", "3x daily"))),
                patient("John").withMedications(List.of(new Medication(null, "500 mg", "3x daily"))));

        for (Patient patient : invalid) {
            assertThrows(IllegalArgumentException.class, () -> service.add(DOCTOR, patient), patient.toString());
        }
        assertEquals(0, repository.saveCalls);
    }

    @Test
    void aBirthDateOfTodayIsAllowed() {
        assertDoesNotThrow(() -> service.add(DOCTOR, patient("John").withBirthDate(LocalDate.of(2026, 1, 1))));
    }

    @Test
    void updateSavesThePatient() {
        startWith(patient("John"), patient("Jane"));
        Patient updated = patient("John").withAddress("Oak Lane 1");

        assertEquals(updated, service.update(DOCTOR, updated));

        assertEquals(updated, repository.findById("john").orElseThrow());
        assertTrue(service.getAll(DOCTOR).contains(updated));
        assertEquals(2, service.getAll(DOCTOR).size());
    }

    @Test
    void updateOfUnknownPatientIsRejected() {
        startWith(patient("John"));

        assertThrows(PatientNotFoundException.class, () -> service.update(DOCTOR, patient("Jane")));
    }

    @Test
    void eachProfessionCanChangeExactlyItsEditableFields() {
        for (Profession profession : Profession.values()) {
            if (profession == Profession.PHARMACIST) {
                continue; // can't edit at all, see below
            }
            for (PatientField field : PatientField.values()) {
                startWith(patient("John"));
                Patient updated = changeField(patient("John"), field);

                if (FieldPermissions.canEdit(profession, field)) {
                    assertDoesNotThrow(() -> service.update(user(profession), updated), profession + " " + field);
                    assertEquals(updated, repository.findById("john").orElseThrow());
                } else {
                    assertThrows(SecurityException.class, () -> service.update(user(profession), updated),
                            profession + " " + field);
                    assertEquals(patient("John"), repository.findById("john").orElseThrow());
                }
            }
        }
    }

    @Test
    void pharmacistCanNotEdit() {
        startWith(patient("John"));

        assertThrows(SecurityException.class,
                () -> service.update(user(Profession.PHARMACIST), patient("John").withAddress("Oak Lane 1")));
    }

    @Test
    void careTeamCanNotBeChanged() {
        startWith(patient("John"));
        Map<String, Boolean> biggerTeam = teamOfEveryProfession();
        biggerTeam.put(OUTSIDER.id(), true);

        assertThrows(SecurityException.class,
                () -> service.update(DOCTOR, patient("John").withCareTeam(biggerTeam)));
        assertEquals(0, repository.saveCalls);
    }

    @Test
    void addPutsTheCreatorOnTheCareTeam() {
        Patient added = service.add(OUTSIDER, patient(null, "John").withCareTeam(Map.of("pharmacist", true)));

        assertTrue(added.hasMember(OUTSIDER.id()));
        assertTrue(added.hasMember("pharmacist"));
        assertEquals(List.of(added), repository.getStored());
        assertEquals(List.of(added), service.getAll(OUTSIDER));
    }

    @Test
    void nonMembersDoNotSeeThePatient() {
        Patient johnsTeamOnly = patient("John");
        Patient outsidersPatient = patient("Jane").withCareTeam(Map.of(OUTSIDER.id(), true));
        startWith(johnsTeamOnly, outsidersPatient);

        assertEquals(List.of(johnsTeamOnly), service.getAll(DOCTOR));
        assertEquals(List.of(outsidersPatient), service.getAll(OUTSIDER));
    }

    @Test
    void getReturnsAPatientOfTheCareTeam() {
        startWith(patient("John"), patient("Jane"));

        assertEquals(patient("Jane"), service.get(DOCTOR, "jane"));
    }

    @Test
    void getOfANonMembersPatientIsNotFound() {
        startWith(patient("John"));

        assertThrows(PatientNotFoundException.class, () -> service.get(OUTSIDER, "john"));
        assertThrows(PatientNotFoundException.class, () -> service.get(DOCTOR, "nobody"));
    }

    @Test
    void nonMembersCanNotEditThePatient() {
        startWith(patient("John"));

        assertThrows(SecurityException.class,
                () -> service.update(OUTSIDER, patient("John").withAddress("Oak Lane 1")));
        assertEquals(0, repository.saveCalls);
    }

    @Test
    void falseInTheCareTeamIsNotMembership() {
        startWith(patient("John").withCareTeam(Map.of("doctor", false)));

        assertTrue(service.getAll(DOCTOR).isEmpty());
    }

    @Test
    void updateRejectsInvalidValues() {
        startWith(patient("John"));

        assertThrows(IllegalArgumentException.class, () -> service.update(DOCTOR, patient("John").withHeight(0)));
        assertThrows(IllegalArgumentException.class, () -> service.update(DOCTOR, patient("John").withFirstName("")));
        assertThrows(IllegalArgumentException.class,
                () -> service.update(DOCTOR, patient("John").withBirthDate(LocalDate.of(2027, 1, 1))));
        assertEquals(0, repository.saveCalls);
    }

    @Test
    void doctorCanUpdateMedication() {
        startWith(patient("John"));
        Patient updated = patient("John").withMedications(List.of(new Medication("Paracetamol", "500 mg", "3x daily")));

        service.update(DOCTOR, updated);

        assertEquals(List.of(updated), service.getAll(DOCTOR));
    }

    @Test
    void medicationUpdateIsRejectedForProfessionsOtherThanDoctor() {
        startWith(patient("John"));
        Patient updated = patient("John").withMedications(List.of(new Medication("Paracetamol", "500 mg", "3x daily")));

        for (Profession profession : List.of(Profession.PHARMACIST, Profession.PHYSIOTHERAPIST, Profession.DENTIST)) {
            assertThrows(SecurityException.class, () -> service.update(user(profession), updated));
        }
        assertEquals(List.of(patient("John")), service.getAll(DOCTOR));
    }

    @Test
    void getAllIsAllowedForEveryProfession() {
        service.add(DOCTOR, patient("John"));

        for (Profession profession : Profession.values()) {
            assertEquals(List.of(patient("John")), service.getAll(user(profession)));
        }
    }
}
