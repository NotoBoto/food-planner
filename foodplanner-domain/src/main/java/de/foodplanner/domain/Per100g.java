package de.foodplanner.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record Per100g(BigDecimal kcal, BigDecimal protein, BigDecimal fat, BigDecimal carbs) {

    public Per100g {
        Objects.requireNonNull(kcal, "kcal");
        Objects.requireNonNull(protein, "protein");
        Objects.requireNonNull(fat, "fat");
        Objects.requireNonNull(carbs, "carbs");
    }
}
