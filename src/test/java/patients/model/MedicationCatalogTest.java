package patients.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MedicationCatalogTest {

    @Test
    void everyTypeHasEightMedications() {
        for (MedicationType type : MedicationType.values()) {
            assertEquals(8, Arrays.stream(MedicationCatalog.values()).filter(m -> m.type == type).count(),
                    type.toString());
        }
    }

    @Test
    void labelsAreUnique() {
        assertEquals(MedicationCatalog.values().length,
                Arrays.stream(MedicationCatalog.values()).map(m -> m.label).distinct().count());
    }

    @Test
    void containsMatchesTheExactLabel() {
        assertTrue(MedicationCatalog.contains("Paracetamol"));
        assertTrue(MedicationCatalog.contains("Vitamin B12"));
        assertFalse(MedicationCatalog.contains("paracetamol"));
        assertFalse(MedicationCatalog.contains("PARACETAMOL"));
        assertFalse(MedicationCatalog.contains("Aspirin"));
        assertFalse(MedicationCatalog.contains(null));
    }
}
