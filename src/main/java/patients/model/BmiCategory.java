package patients.model;

public enum BmiCategory {
    UNDERWEIGHT("Underweight"),
    HEALTHY("Healthy weight"),
    OVERWEIGHT("Overweight"),
    OBESE("Obese");

    public final String label;

    private BmiCategory(String label) {
        this.label = label;
    }

    public static BmiCategory of(double bmi) {
        if (bmi < 18.5) return UNDERWEIGHT;
        if (bmi < 25) return HEALTHY;
        if (bmi < 30) return OVERWEIGHT;
        return OBESE;
    }
}
