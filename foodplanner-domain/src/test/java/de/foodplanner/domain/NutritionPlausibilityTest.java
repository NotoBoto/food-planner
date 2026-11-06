package de.foodplanner.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class NutritionPlausibilityTest {

    private static Per100g of(String kcal, String protein, String fat, String carbs) {
        return new Per100g(new BigDecimal(kcal), new BigDecimal(protein), new BigDecimal(fat), new BigDecimal(carbs));
    }

    @Test
    void acceptsNormalValues() {
        assertThat(NutritionPlausibility.isPlausible(of("250", "8", "10", "30"))).isTrue();
    }

    @Test
    void acceptsBoundaries() {
        assertThat(NutritionPlausibility.isPlausible(of("0", "0", "0", "0"))).isTrue();
        assertThat(NutritionPlausibility.isPlausible(of("900", "0", "100", "5"))).isTrue();
    }

    @Test
    void rejectsKcalOutOfRange() {
        assertThat(NutritionPlausibility.isPlausible(of("901", "1", "1", "1"))).isFalse();
        assertThat(NutritionPlausibility.isPlausible(of("-1", "1", "1", "1"))).isFalse();
    }

    @Test
    void rejectsMacroOutOfRange() {
        assertThat(NutritionPlausibility.isPlausible(of("100", "0", "100.1", "0"))).isFalse();
        assertThat(NutritionPlausibility.isPlausible(of("100", "0", "-0.1", "0"))).isFalse();
    }

    @Test
    void rejectsSumOver105() {
        assertThat(NutritionPlausibility.isPlausible(of("500", "5.1", "50", "50"))).isFalse();
        assertThat(NutritionPlausibility.isPlausible(of("500", "5", "50", "50"))).isTrue();
    }

    @Test
    void rejectsNull() {
        assertThat(NutritionPlausibility.isPlausible(null)).isFalse();
    }
}
