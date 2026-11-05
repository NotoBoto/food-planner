package de.foodplanner.domain;

import java.math.BigDecimal;


public final class NutritionPlausibility {

    private static final BigDecimal MAX_KCAL = BigDecimal.valueOf(900);
    private static final BigDecimal MAX_MACRO = BigDecimal.valueOf(100);
    private static final BigDecimal MAX_SUM = BigDecimal.valueOf(105);

    private NutritionPlausibility() {
    }

    public static boolean isPlausible(Per100g nutrition) {
        if (nutrition == null) {
            return false;
        }
        if (!inRange(nutrition.kcal(), MAX_KCAL)
                || !inRange(nutrition.protein(), MAX_MACRO)
                || !inRange(nutrition.fat(), MAX_MACRO)
                || !inRange(nutrition.carbs(), MAX_MACRO)) {
            return false;
        }
        BigDecimal sum = nutrition.protein().add(nutrition.fat()).add(nutrition.carbs());
        return sum.compareTo(MAX_SUM) <= 0;
    }

    private static boolean inRange(BigDecimal value, BigDecimal max) {
        return value.signum() >= 0 && value.compareTo(max) <= 0;
    }
}
