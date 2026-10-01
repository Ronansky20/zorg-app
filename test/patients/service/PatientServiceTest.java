package patients.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import patients.model.Patient;
import patients.model.Profession;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatientServiceTest {

    private InMemoryPatientRepository repository;
    private PatientService service;

    private static Patient patient(String firstName) {
        return new Patient(firstName, "Doe", 30, 70.0, 1.8, "Elm Street 58",
                new Patient.Specialists("Anon", "Anon", "Anon", "Anon"));
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
        repository = new InMemoryPatientRepository(List.of(patient("John"), patient("Jane")));
        service = new PatientService(repository);
        service.login(Profession.DOCTOR);

        service.load();

        assertEquals(2, service.getAll().size());
    }

    @Test
    void loadReplacesPreviouslyLoadedPatients() {
        repository = new InMemoryPatientRepository(List.of(patient("John")));
        service = new PatientService(repository);
        service.login(Profession.DOCTOR);
        service.load();

        repository.saveAll(List.of(patient("Jane")));
        service.load();

        assertEquals(List.of(patient("Jane")), service.getAll());
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
    void saveForwardsCurrentPatientsToRepository() {
        service.add(patient("John"));

        service.save();

        assertEquals(1, repository.saveAllCalls);
        assertEquals(List.of(patient("John")), repository.getStored());
    }

    @Test
    void addAndGetAllRequireALoggedInUser() {
        service = new PatientService(repository);

        assertThrows(IllegalStateException.class, () -> service.add(patient("John")));
        assertThrows(IllegalStateException.class, () -> service.getAll());
    }

    @Test
    void addAndGetAllRejectProfessionsWithoutPermission() {
        service.login(Profession.PHARMACIST);

        assertThrows(SecurityException.class, () -> service.add(patient("John")));
        assertThrows(SecurityException.class, () -> service.getAll());
    }
}
