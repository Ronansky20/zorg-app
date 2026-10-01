package patients.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PatientTest {

    private static Patient patientWith(double weight, double height) {
        return new Patient("John", "Doe", 28, weight, height, "Elm Street 58",
                new Patient.Specialists("Anon", "Anon", "Anon", "Anon"));
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
