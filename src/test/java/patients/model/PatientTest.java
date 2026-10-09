package patients.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PatientTest {

    private static Patient patientWith(double weight, double height) {
        return patientBornOn(LocalDate.of(1995, 6, 15), weight, height);
    }

    private static Patient patientBornOn(LocalDate birthDate) {
        return patientBornOn(birthDate, 70.0, 1.8);
    }

    private static Patient patientBornOn(LocalDate birthDate, double weight, double height) {
        return new Patient("John", "Doe", birthDate, weight, height, "Elm Street 58",
                new Patient.Specialists("Anon", "Anon", "Anon", "Anon"));
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
}
