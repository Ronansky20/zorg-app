package patients.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatientTest {

    private static Patient patientWith(double weight, double height) {
        return patientBornOn(LocalDate.of(1995, 6, 15), weight, height);
    }

    private static Patient patientBornOn(LocalDate birthDate) {
        return patientBornOn(birthDate, 70.0, 1.8);
    }

    private static Patient patientBornOn(LocalDate birthDate, double weight, double height) {
        return new Patient("id-1", "John", "Doe", birthDate, weight, height, "Elm Street 58",
                new Patient.Specialists("Anon", "Anon", "Anon", "Anon"), List.of());
    }

    @Test
    void ageCountsCompletedYears() {
        Patient patient = patientBornOn(LocalDate.of(1995, 6, 15));

        assertEquals(30, patient.age(LocalDate.of(2026, 6, 14)));
        assertEquals(31, patient.age(LocalDate.of(2026, 6, 15)));
    }

    @Test
    void leapDayBirthdayCountsOnFirstOfMarchInNonLeapYears() {
        Patient patient = patientBornOn(LocalDate.of(2000, 2, 29));

        assertEquals(25, patient.age(LocalDate.of(2026, 2, 28)));
        assertEquals(26, patient.age(LocalDate.of(2026, 3, 1)));
    }

    @Test
    void bmiIsWeightDividedByHeightSquared() {
        Patient patient = patientWith(89.2, 1.9);

        assertEquals(89.2 / (1.9 * 1.9), patient.bmi(), 1e-9);
    }

    @Test
    void bmiOfZeroHeightIsInfinite() {
        Patient patient = patientWith(70.0, 0.0);

        assertEquals(Double.POSITIVE_INFINITY, patient.bmi());
    }

    @Test
    void recordsWithSameValuesAreEqual() {
        Patient a = patientWith(70.0, 1.8);
        Patient b = patientWith(70.0, 1.8);

        assertEquals(a, b);
    }

    private static final Medication PARACETAMOL = new Medication("Paracetamol", "500 mg", "3x daily");
    private static final Medication IBUPROFEN = new Medication("Ibuprofen", "400 mg", "as needed");

    @Test
    void nullMedicationsBecomeAnEmptyList() {
        Patient patient = new Patient("id-1", "John", "Doe", LocalDate.of(1995, 6, 15), 70.0, 1.8, "Elm Street 58",
                new Patient.Specialists("Anon", "Anon", "Anon", "Anon"), null);

        assertTrue(patient.medications().isEmpty());
    }

    @Test
    void medicationsAreACopyThatCannotBeModified() {
        List<Medication> original = new ArrayList<>(List.of(PARACETAMOL));
        Patient patient = patientWith(70.0, 1.8).withMedications(original);

        original.add(IBUPROFEN);

        assertEquals(List.of(PARACETAMOL), patient.medications());
        assertThrows(UnsupportedOperationException.class, () -> patient.medications().add(IBUPROFEN));
    }

    @Test
    void withMedicationsReplacesOnlyMedications() {
        Patient patient = patientWith(70.0, 1.8);

        Patient updated = patient.withMedications(List.of(PARACETAMOL, IBUPROFEN));

        assertEquals(List.of(PARACETAMOL, IBUPROFEN), updated.medications());
        assertEquals(patient, updated.withMedications(List.of()));
    }

    @Test
    void otherMutatorsKeepMedications() {
        Patient patient = patientWith(70.0, 1.8).withMedications(List.of(PARACETAMOL));

        assertEquals(List.of(PARACETAMOL), patient.withFirstName("Jane").medications());
        assertEquals(List.of(PARACETAMOL), patient.withLastName("Smith").medications());
        assertEquals(List.of(PARACETAMOL), patient.withBirthDate(LocalDate.of(2000, 1, 1)).medications());
        assertEquals(List.of(PARACETAMOL), patient.withWeight(80.0).medications());
        assertEquals(List.of(PARACETAMOL), patient.withHeight(1.9).medications());
        assertEquals(List.of(PARACETAMOL), patient.withAddress("Oak Lane 1").medications());
    }

    @Test
    void withIdReplacesOnlyTheId() {
        Patient patient = patientWith(70.0, 1.8);

        Patient updated = patient.withId("id-2");

        assertEquals("id-2", updated.id());
        assertEquals(patient, updated.withId("id-1"));
    }

    @Test
    void otherMutatorsKeepTheId() {
        Patient patient = patientWith(70.0, 1.8);

        assertEquals("id-1", patient.withFirstName("Jane").id());
        assertEquals("id-1", patient.withLastName("Smith").id());
        assertEquals("id-1", patient.withBirthDate(LocalDate.of(2000, 1, 1)).id());
        assertEquals("id-1", patient.withWeight(80.0).id());
        assertEquals("id-1", patient.withHeight(1.9).id());
        assertEquals("id-1", patient.withAddress("Oak Lane 1").id());
        assertEquals("id-1", patient.withMedications(List.of(PARACETAMOL)).id());
    }
}
