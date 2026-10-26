package patients.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BmiCategoryTest {

    @Test
    void categoriesFollowTheWhoBoundaries() {
        assertEquals(BmiCategory.UNDERWEIGHT, BmiCategory.of(18.4));
        assertEquals(BmiCategory.HEALTHY, BmiCategory.of(18.5));
        assertEquals(BmiCategory.HEALTHY, BmiCategory.of(24.9));
        assertEquals(BmiCategory.OVERWEIGHT, BmiCategory.of(25.0));
        assertEquals(BmiCategory.OVERWEIGHT, BmiCategory.of(29.9));
        assertEquals(BmiCategory.OBESE, BmiCategory.of(30.0));
    }
}
