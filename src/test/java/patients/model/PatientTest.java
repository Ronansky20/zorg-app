package patients.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
                Map.of("jansen", true), List.of());
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
                Map.of(), null);

        assertTrue(patient.medications().isEmpty());
    }

    @Test
    void nullCareTeamBecomesAnEmptyMap() {
        Patient patient = new Patient("id-1", "John", "Doe", LocalDate.of(1995, 6, 15), 70.0, 1.8, "Elm Street 58",
                null, List.of());

        assertTrue(patient.careTeam().isEmpty());
        assertFalse(patient.hasMember("jansen"));
    }

    @Test
    void hasMemberIsTrueOnlyForUsersMarkedTrue() {
        Patient patient = patientWith(70.0, 1.8).withCareTeam(Map.of("jansen", true, "devries", false));

        assertTrue(patient.hasMember("jansen"));
        assertFalse(patient.hasMember("devries"));
        assertFalse(patient.hasMember("visser"));
    }

    @Test
    void careTeamIsACopyThatCannotBeModified() {
        Map<String, Boolean> original = new HashMap<>(Map.of("jansen", true));
        Patient patient = patientWith(70.0, 1.8).withCareTeam(original);

        original.put("visser", true);

        assertEquals(Map.of("jansen", true), patient.careTeam());
        assertThrows(UnsupportedOperationException.class, () -> patient.careTeam().put("visser", true));
    }

    @Test
    void otherMutatorsKeepTheCareTeam() {
        Patient patient = patientWith(70.0, 1.8);

        assertEquals(Map.of("jansen", true), patient.withFirstName("Jane").careTeam());
        assertEquals(Map.of("jansen", true), patient.withAddress("Oak Lane 1").careTeam());
        assertEquals(Map.of("jansen", true), patient.withMedications(List.of(PARACETAMOL)).careTeam());
        assertEquals(Map.of("jansen", true), patient.withId("id-2").careTeam());
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
